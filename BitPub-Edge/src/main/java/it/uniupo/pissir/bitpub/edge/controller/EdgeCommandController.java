/**
 * Autore: Timothy Giolito 20054431
 */
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
 * Endpoint REST per i comandi in ingresso dalla WebApp, ovvero le azioni interattive e i risultati dei tornei.
 * La WebApp contatta l'Edge, raggiungibile nella rete locale anche in caso di down temporaneo del Cloud;
 * l'Edge a sua volta valida il JWT e inoltra il comando al Cloud tramite MQTT in QoS1.
 * Ci appoggiamo al buffer offline per accodare i comandi localmente se la connessione è caduta.
 * Rispondiamo sempre con 202, lasciando che lo stato si aggiorni in autonomia sulla WebApp tramite i topic dedicati.
 */
@RestController
@RequestMapping("/edge")
@CrossOrigin(origins = "*") // Apertura CORS per la demo, per la produzione andrà limitato all'origine della WebApp
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
     * Gestisce l'azione interattiva inviata dal giocatore.
     * L'Edge risolve localmente chi sta agendo e manda l'azione direttamente al simulatore, senza passare per il Cloud.
     * Questo sblocca il turno anche se il Cloud è irraggiungibile. Ritorniamo 202 e lo stato aggiornato
     * arriverà alla WebApp tramite il topic match-state.
     */
    @PostMapping("/matches/{matchId}/action")
    public ResponseEntity<String> gameAction(@PathVariable("matchId") String matchId,
                                             @RequestBody String actionJson,
                                             @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Actor actor = authenticate(authHeader);

        // Controllo il turno basandomi sullo stato locale, che per noi è la fonte di verità.
        // Se non è il turno di chi ha fatto la chiamata, blocco l'azione prima che arrivi al simulatore.
        if (!ruleEngine.isPlayersTurn(matchId, actor.userId())) {
            log.info("Blocked out-of-turn action for match {} by actor {}", matchId, actor.userId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non e' il tuo turno");
        }

        LocalMatchState state = ruleEngine.getState(matchId);
        if (state == null || state.gameInstanceId == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Partita non attiva sull'Edge");
        }

        // Ricavo la squadra di chi agisce partendo dall'indice del giocatore
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
        request.put("eventId", body.path("eventId").asText(null)); // Manteniamo l'id per garantire l'idempotenza

        String topic = MqttTopics.getSimulatorActionTopic(state.gameInstanceId);
        localMqttOutboundChannel.send(MessageBuilder.withPayload(toJson(request))
                .setHeader(MqttHeaders.TOPIC, topic)
                .build());
        log.info("Game action for match {} routed to local simulator via {} (actor {}, team {})",
                matchId, topic, actor.userId(), team);
        return ResponseEntity.accepted().body("{\"status\":\"ACCEPTED\"}");
    }

    /**
     * Risultato della partita riportato da un amministratore di locale.
     * Viene inviato al Cloud via MQTT per poi avanzare il tabellone del torneo.
     * Ritorniamo 202, e il tabellone aggiornato sarà inviato alla WebApp in un secondo momento.
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
     * Comando generico di creazione, aggiornamento o eliminazione per le entità del cloud.
     * Dopo aver validato il token, l'Edge inoltra il payload incapsulato in un wrapper via MQTT.
     * Ritorniamo 202 per confermare l'accettazione della richiesta.
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

    // ── Utility interne ─────────────────────────────────────────────────────────

    private record Actor(String userId, String role) {}

    /**
     * Prepara il pacchetto con identità e payload, accodandolo nel buffer in attesa dell'invio al Cloud.
     * Se la connessione è assente, il messaggio resta al sicuro localmente.
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
