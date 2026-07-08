package it.uniupo.pissir.bitpub.edge.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.common.mqtt.TournamentResultCommand;
import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import it.uniupo.pissir.bitpub.edge.service.RuleEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Inbound REST ingress for WebApp commands: interactive game actions and tournament match results.
 * The WebApp calls the Edge (reachable on the local network even when the cloud is briefly down);
 * the Edge validates the caller's JWT once (trusted origin) and relays the command to the Cloud
 * 100% over MQTT (QoS1). The broker durably queues the command while the Cloud subscriber is down,
 * so there is no app-level buffer or REST fallback. Everything returns 202 — the resulting state
 * reaches the WebApp on its own live MQTT topic (match-state / tournament-state).
 */
@RestController
@RequestMapping("/edge")
@CrossOrigin(origins = "*") // ponytail: demo-open CORS; lock to the WebApp origin for production
@Slf4j
public class EdgeCommandController {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final MessageChannel cloudMqttOutboundChannel;
    private final RuleEngineService ruleEngine;

    public EdgeCommandController(JwtUtils jwtUtils, ObjectMapper objectMapper,
                                @Qualifier("cloudMqttOutboundChannel") MessageChannel cloudMqttOutboundChannel,
                                RuleEngineService ruleEngine) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
        this.cloudMqttOutboundChannel = cloudMqttOutboundChannel;
        this.ruleEngine = ruleEngine;
    }

    /**
     * Interactive game action from the turn player. Body = GameActionRequestDto JSON incl. eventId.
     * Published to the cloud over MQTT (QoS1); identity is packed into the wrapper (MQTT has no
     * headers). Returns 202 — the resulting state reaches the WebApp via the match-state MQTT topic.
     */
    @PostMapping("/matches/{matchId}/action")
    public ResponseEntity<String> gameAction(@PathVariable("matchId") String matchId,
                                             @RequestBody String actionJson,
                                             @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Actor actor = authenticate(authHeader);

        // Gate del turno sullo stato live locale (autoritativo sull'Edge): se non e' il turno del
        // chiamante l'azione e' bloccata qui, senza nemmeno raggiungere il simulatore/Cloud.
        if (!ruleEngine.isPlayersTurn(matchId, actor.userId())) {
            log.info("Blocked out-of-turn action for match {} by actor {}", matchId, actor.userId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non e' il tuo turno");
        }

        String topic = MqttTopics.getCloudMatchActionTopic(matchId);
        publishCommand(topic, matchId, actor, actionJson);
        log.info("Game action for match {} published to cloud via MQTT {} (actor {})", matchId, topic, actor.userId());
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
        publishCommand(topic, tournamentId, actor, payload);
        log.info("Tournament result for {}/{} published to cloud via MQTT {} (actor {})",
                tournamentId, tMatchId, topic, actor.userId());
        return ResponseEntity.accepted().body("{\"status\":\"ACCEPTED\"}");
    }

    // ── internals ───────────────────────────────────────────────────────────────

    private record Actor(String userId, String role) {}

    /** Packs identity + body into an MqttCommandWrapper and publishes it to the cloud command topic (QoS1). */
    private void publishCommand(String topic, String targetId, Actor actor, String payloadJson) {
        String json = toJson(new MqttCommandWrapper(actor.userId(), actor.role(), targetId, payloadJson));
        cloudMqttOutboundChannel.send(MessageBuilder.withPayload(json)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build());
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
