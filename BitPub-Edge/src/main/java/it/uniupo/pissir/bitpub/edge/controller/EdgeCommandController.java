package it.uniupo.pissir.bitpub.edge.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.common.mqtt.TournamentResultCommand;
import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import it.uniupo.pissir.bitpub.edge.service.MqttBufferService;
import it.uniupo.pissir.bitpub.edge.service.RuleEngineService;
import it.uniupo.pissir.bitpub.edge.service.RuleEngineService.LocalMatchState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inbound REST ingress for WebApp commands: interactive game actions and tournament match results.
 * The WebApp calls the Edge (reachable on the local network even when the cloud is briefly down);
 * the Edge validates the caller's JWT once (trusted origin) and relays the command to the Cloud
 * 100% over MQTT (QoS1) through the explicit offline buffer (MqttBufferService), which queues the
 * command locally while the Cloud connection is DOWN and flushes it on reconnect. Everything returns
 * 202 — the resulting state reaches the WebApp on its own live MQTT topic (match-state / tournament-state).
 */
@RestController
@RequestMapping("/edge")
@CrossOrigin(origins = "*") // ponytail: demo-open CORS; lock to the WebApp origin for production
@Slf4j
public class EdgeCommandController {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final MqttBufferService mqttBuffer;
    private final RuleEngineService ruleEngine;
    private final MessageChannel localMqttOutboundChannel;

    public EdgeCommandController(JwtUtils jwtUtils, ObjectMapper objectMapper,
                                MqttBufferService mqttBuffer,
                                RuleEngineService ruleEngine,
                                @Qualifier("localMqttOutboundChannel") MessageChannel localMqttOutboundChannel) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
        this.mqttBuffer = mqttBuffer;
        this.ruleEngine = ruleEngine;
        this.localMqttOutboundChannel = localMqttOutboundChannel;
    }

    /**
     * Interactive game action from the turn player. Body = GameActionRequestDto JSON incl. eventId.
     * L'Edge NON delega piu' l'orchestrazione al Cloud: risolve localmente la squadra di chi agisce e
     * inoltra l'azione DIRETTAMENTE al simulatore locale (topic bitpub/simulators/{gameInstanceId}/action)
     * su un canale MQTT non bufferizzato. Il simulatore ripubblica il SensorEvent in locale, sbloccando
     * il turno nel RuleEngineService anche a Cloud irraggiungibile. Returns 202 — lo stato risultante
     * raggiunge la WebApp sul topic match-state.
     */
    @PostMapping("/matches/{matchId}/action")
    public ResponseEntity<String> gameAction(@PathVariable("matchId") String matchId,
                                             @RequestBody String actionJson,
                                             @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Actor actor = authenticate(authHeader);

        // Gate del turno sullo stato live locale (autoritativo sull'Edge): se non e' il turno del
        // chiamante l'azione e' bloccata qui, senza nemmeno raggiungere il simulatore.
        if (!ruleEngine.isPlayersTurn(matchId, actor.userId())) {
            log.info("Blocked out-of-turn action for match {} by actor {}", matchId, actor.userId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non e' il tuo turno");
        }

        LocalMatchState state = ruleEngine.getState(matchId);
        if (state == null || state.gameInstanceId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Partita non attiva sull'Edge");
        }

        // Risolvi la squadra di chi agisce: indice dello userId in playerUserIds -> teamOrder omologo.
        int idx = state.playerUserIds.indexOf(actor.userId());
        String team = (idx >= 0 && idx < state.teamOrder.size())
                ? state.teamOrder.get(idx)
                : (state.teamOrder.isEmpty() ? null : state.teamOrder.get(0));

        JsonNode body = parseJson(actionJson);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("gameInstanceId", state.gameInstanceId);
        request.put("localeId", state.localeId);
        request.put("gameTypeId", state.gameTypeId);
        request.put("matchId", matchId);
        request.put("sensorType", body.path("sensorType").asText(null));
        request.put("team", team);
        request.put("eventId", body.path("eventId").asText(null)); // idempotenza propagata al sensor event

        String topic = MqttTopics.getSimulatorActionTopic(state.gameInstanceId);
        localMqttOutboundChannel.send(MessageBuilder.withPayload(toJson(request))
                .setHeader(MqttHeaders.TOPIC, topic)
                .build());
        log.info("Game action for match {} routed to local simulator via {} (actor {}, team {})",
                matchId, topic, actor.userId(), team);
        return ResponseEntity.accepted().body("{\"status\":\"ACCEPTED\"}");
    }

    /**
     * Tournament match result reported by a LOCALE_ADMIN. Published to the cloud over MQTT (QoS1);
     * tournament-service validates the role from the wrapper and advances the bracket. Returns 202 —
     * the updated bracket reaches the WebApp via the tournament-state MQTT topic.
     */
    @PutMapping("/tournaments/{tournamentId}/matches/{tMatchId}/result")
    public ResponseEntity<String> tournamentResult(@PathVariable("tournamentId") String tournamentId,
                                                   @PathVariable("tMatchId") String tMatchId,
                                                   @RequestParam("winnerId") String winnerId,
                                                   @RequestParam(name = "stats", required = false) String stats,
                                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Actor actor = authenticate(authHeader);
        String topic = MqttTopics.getCloudTournamentResultTopic(tournamentId, tMatchId);
        String payload = toJson(new TournamentResultCommand(tMatchId, winnerId, stats));
        publishCommand(topic, tournamentId, actor, payload, "result for Tournament " + tournamentId + "/" + tMatchId);
        log.info("Tournament result for {}/{} published to cloud via MQTT {} (actor {})",
                tournamentId, tMatchId, topic, actor.userId());
        return ResponseEntity.accepted().body("{\"status\":\"ACCEPTED\"}");
    }

    /**
     * Generic CUD (Create/Update/Delete) command for any cloud entity (users, locales, catalog, ...).
     * The WebApp posts the raw action body; the Edge validates the JWT once and relays it to the Cloud
     * 100% over MQTT (QoS1), packed in an MqttCommandWrapper. {entity} is echoed in the topic so each
     * cloud service subscribes only to its own slice. Returns 202 — result flows back on the entity's
     * own live MQTT topic. The action verb (CREATE/UPDATE/DELETE) travels inside the body JSON.
     */
    @PostMapping("/system/{entity}/action")
    public ResponseEntity<String> systemAction(@PathVariable("entity") String entity,
                                               @RequestBody String actionJson,
                                               @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Actor actor = authenticate(authHeader);
        String topic = MqttTopics.getCloudSystemActionTopic(entity);
        publishCommand(topic, entity, actor, actionJson, "system action for " + entity);
        log.info("System CUD action for entity {} published to cloud via MQTT {} (actor {})", entity, topic, actor.userId());
        return ResponseEntity.accepted().body("{\"status\":\"ACCEPTED\"}");
    }

    // ── internals ───────────────────────────────────────────────────────────────

    private record Actor(String userId, String role) {}

    /**
     * Packs identity + body into an MqttCommandWrapper and relays it to the cloud command topic (QoS1)
     * through the explicit offline buffer, which queues it locally if the Cloud connection is DOWN.
     */
    private void publishCommand(String topic, String targetId, Actor actor, String payloadJson, String description) {
        String json = toJson(new MqttCommandWrapper(actor.userId(), actor.role(), targetId, payloadJson));
        mqttBuffer.send(MessageBuilder.withPayload(json)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build(), description);
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corpo azione non valido");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize command");
        }
    }

    private Actor authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return new Actor(jwtUtils.getUserIdFromToken(token), jwtUtils.getRoleFromToken(token));
    }
}
