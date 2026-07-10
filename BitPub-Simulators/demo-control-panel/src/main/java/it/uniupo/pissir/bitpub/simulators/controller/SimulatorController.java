/**
 * autore Timothy Giolito 20054431
 *
 * Controller principale per l'invio degli eventi generati dal simulatore.
 * Raccoglie le richieste di inizio partita e le interazioni di gioco,
 * delegando poi al servizio sottostante la pubblicazione degli eventi elaborati.
 */
package it.uniupo.pissir.bitpub.simulators.controller;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.simulators.service.GenericSimulator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/simulators")
@CrossOrigin(origins = "*")
public class SimulatorController {

    private final GenericSimulator simulator;

    public SimulatorController(GenericSimulator simulator) {
        this.simulator = simulator;
    }

    /**
     * Avvia una partita pubblicando MATCH_START con i nomi dei giocatori: il match-service
     * auto-crea il match con questi nomi. POST /{gameType}/{localeId}/{gameInstanceId}/start-match
     */
    @PostMapping("/{gameType}/{localeId}/{gameInstanceId}/start-match")
    public ResponseEntity<?> startMatchWithNames(
            @PathVariable("gameType") String gameType,
            @PathVariable("localeId") String localeId,
            @PathVariable("gameInstanceId") String gameInstanceId,
            @RequestBody(required = false) Map<String, Object> body) {

        if (body == null) body = Map.of();
        Map<String, Object> payload = new HashMap<>();
        payload.put("teamAName", body.getOrDefault("teamAName", "RED").toString());
        payload.put("teamBName", body.getOrDefault("teamBName", "BLUE").toString());

        publish(gameInstanceId, null, "MATCH_START", localeId, payload);
        return ResponseEntity.ok(Map.of("status", "MATCH_STARTED", "gameInstanceId", gameInstanceId));
    }

    /**
     * Pubblica un evento arbitrario (MATCH_END, o un evento di gioco con payload gia' pronto).
     * POST /{gameType}/{localeId}/{gameInstanceId}/event?eventType=MATCH_END
     */
    @PostMapping("/{gameType}/{localeId}/{gameInstanceId}/event")
    public ResponseEntity<?> triggerEvent(
            @PathVariable("gameType") String gameType,
            @PathVariable("localeId") String localeId,
            @PathVariable("gameInstanceId") String gameInstanceId,
            @RequestParam("eventType") String eventType,
            @RequestParam(value = "matchId", required = false) String matchId,
            @RequestBody(required = false) Map<String, Object> payload) {

        publish(gameInstanceId, matchId, eventType, localeId, payload != null ? payload : new HashMap<>());
        return ResponseEntity.ok(Map.of("status", "PUBLISHED", "eventType", eventType));
    }

    private void publish(String gameInstanceId, String matchId, String type, String localeId, Map<String, Object> payload) {
        SensorEvent event = SensorEvent.builder()
                .eventId(UUID.randomUUID())
                .gameInstanceId(gameInstanceId)
                .matchId(matchId)
                .sensorType(type)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
        simulator.publish(event, localeId, gameInstanceId);
    }
}
