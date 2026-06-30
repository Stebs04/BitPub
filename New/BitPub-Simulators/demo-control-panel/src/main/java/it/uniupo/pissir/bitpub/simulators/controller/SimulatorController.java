package it.uniupo.pissir.bitpub.simulators.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.simulators.biliardo.BilliardsEventGenerator;
import it.uniupo.pissir.bitpub.simulators.calciobalilla.FoosballEventGenerator;
import it.uniupo.pissir.bitpub.simulators.config.MqttConfig.MqttPublisher;
import it.uniupo.pissir.bitpub.simulators.freccette.DartEventGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/simulators")
@CrossOrigin(origins = "*") // For demo purposes
public class SimulatorController {

    private final MqttPublisher mqttPublisher;
    private final ObjectMapper objectMapper;
    private final FoosballEventGenerator foosballGenerator = new FoosballEventGenerator();
    private final DartEventGenerator dartGenerator = new DartEventGenerator();
    private final BilliardsEventGenerator billiardsGenerator = new BilliardsEventGenerator();

    public SimulatorController(MqttPublisher mqttPublisher, ObjectMapper objectMapper) {
        this.mqttPublisher = mqttPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Starts a match by publishing a MATCH_START event that includes the player names.
     * The match-service will auto-create the match with these names on the leaderboard.
     *
     * POST /api/simulators/{gameType}/{localeId}/{gameInstanceId}/start-match
     * Body: { "teamAName": "Marco", "teamBName": "Luigi" }
     */
    @PostMapping("/{gameType}/{localeId}/{gameInstanceId}/start-match")
    public ResponseEntity<?> startMatchWithNames(
            @PathVariable("gameType") String gameType,
            @PathVariable("localeId") String localeId,
            @PathVariable("gameInstanceId") String gameInstanceId,
            @RequestBody(required = false) Map<String, Object> body) {

        if (body == null) body = Map.of();

        String teamAName = body.getOrDefault("teamAName", "RED").toString();
        String teamBName = body.getOrDefault("teamBName", "BLUE").toString();

        // Build a MATCH_START SensorEvent with team names embedded in the payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("teamAName", teamAName);
        payload.put("teamBName", teamBName);

        SensorEvent event = SensorEvent.builder()
                .eventId(UUID.randomUUID())
                .gameInstanceId(gameInstanceId)
                .sensorType("MATCH_START")
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        try {
            String topic = MqttTopics.getSensorEventTopic(localeId, gameInstanceId);
            String jsonPayload = objectMapper.writeValueAsString(event);
            mqttPublisher.sendToMqtt(jsonPayload, topic);
            return ResponseEntity.ok(Map.of(
                    "status", "MATCH_STARTED",
                    "gameType", gameType,
                    "gameInstanceId", gameInstanceId,
                    "teamAName", teamAName,
                    "teamBName", teamBName
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    /**
     * Generic event trigger (GOAL, BALL_POCKETED, DART_HIT, FOUL, MATCH_END, etc.)
     * POST /api/simulators/{gameType}/{localeId}/{gameInstanceId}/event?eventType=GOAL
     */
    @PostMapping("/{gameType}/{localeId}/{gameInstanceId}/event")
    public ResponseEntity<?> triggerEvent(
            @PathVariable("gameType") String gameType,
            @PathVariable("localeId") String localeId,
            @PathVariable("gameInstanceId") String gameInstanceId,
            @RequestParam("eventType") String eventType,
            @RequestParam(value = "matchId", required = false) String matchId,
            @RequestBody(required = false) Map<String, Object> payload) {

        SensorEvent event = null;
        if (payload == null) {
            payload = Map.of();
        }

        switch (gameType.toLowerCase()) {
            case "calciobalilla":
                if ("GOAL".equalsIgnoreCase(eventType)) {
                    event = foosballGenerator.generateGoalEvent(gameInstanceId, matchId, (String) payload.getOrDefault("team", "RED"));
                } else if ("MATCH_START".equalsIgnoreCase(eventType)) {
                    event = foosballGenerator.generateMatchStartEvent(gameInstanceId, matchId);
                } else if ("MATCH_END".equalsIgnoreCase(eventType)) {
                    event = foosballGenerator.generateMatchEndEvent(gameInstanceId, matchId);
                }
                break;
            case "freccette":
                if ("DART_HIT".equalsIgnoreCase(eventType)) {
                    int score = payload.containsKey("score") ? Integer.parseInt(payload.get("score").toString()) : 20;
                    int multiplier = payload.containsKey("multiplier") ? Integer.parseInt(payload.get("multiplier").toString()) : 1;
                    event = dartGenerator.generateDartHitEvent(gameInstanceId, matchId, score, multiplier);
                } else if ("MATCH_START".equalsIgnoreCase(eventType)) {
                    event = dartGenerator.generateMatchStartEvent(gameInstanceId, matchId);
                } else if ("MATCH_END".equalsIgnoreCase(eventType)) {
                    event = dartGenerator.generateMatchEndEvent(gameInstanceId, matchId);
                }
                break;
            case "biliardo":
                if ("BALL_POCKETED".equalsIgnoreCase(eventType)) {
                    int pocketId = payload.containsKey("pocketId") ? Integer.parseInt(payload.get("pocketId").toString()) : 1;
                    int ballNumber = payload.containsKey("ballNumber") ? Integer.parseInt(payload.get("ballNumber").toString()) : 1;
                    String color = (String) payload.getOrDefault("ballColor", "RED");
                    event = billiardsGenerator.generateBallPocketedEvent(gameInstanceId, matchId, pocketId, ballNumber, color);
                } else if ("FOUL".equalsIgnoreCase(eventType)) {
                    // FOUL event for billiards
                    event = SensorEvent.builder()
                            .eventId(UUID.randomUUID())
                            .gameInstanceId(gameInstanceId)
                            .matchId(matchId)
                            .sensorType("FOUL")
                            .timestamp(Instant.now())
                            .payload(Map.of("reason", payload.getOrDefault("reason", "scratch")))
                            .build();
                } else if ("MATCH_START".equalsIgnoreCase(eventType)) {
                    event = billiardsGenerator.generateMatchStartEvent(gameInstanceId, matchId);
                } else if ("MATCH_END".equalsIgnoreCase(eventType)) {
                    event = billiardsGenerator.generateMatchEndEvent(gameInstanceId, matchId);
                }
                break;
            default:
                return ResponseEntity.badRequest().body("Unknown gameType: " + gameType);
        }

        if (event == null) {
            return ResponseEntity.badRequest().body("Unknown eventType or invalid payload for gameType=" + gameType);
        }

        try {
            String topic = MqttTopics.getSensorEventTopic(localeId, gameInstanceId);
            String jsonPayload = objectMapper.writeValueAsString(event);
            mqttPublisher.sendToMqtt(jsonPayload, topic);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
