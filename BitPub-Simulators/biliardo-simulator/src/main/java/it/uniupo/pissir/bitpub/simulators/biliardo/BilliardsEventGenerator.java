package it.uniupo.pissir.bitpub.simulators.biliardo;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class BilliardsEventGenerator {
    
    public SensorEvent generateBallPocketedEvent(String gameInstanceId, String matchId, int pocketId, int ballNumber, String ballColor) {
        return SensorEvent.builder()
                .eventId(UUID.randomUUID())
                .gameInstanceId(gameInstanceId)
                .matchId(matchId)
                .sensorType("BALL_POCKETED")
                .timestamp(Instant.now())
                .payload(Map.of(
                        "pocketId", pocketId,
                        "ballNumber", ballNumber,
                        "ballColor", ballColor
                ))
                .build();
    }
    
    public SensorEvent generateMatchStartEvent(String gameInstanceId, String matchId) {
        return buildEvent(gameInstanceId, matchId, "MATCH_START", Map.of());
    }

    public SensorEvent generateMatchEndEvent(String gameInstanceId, String matchId) {
        return buildEvent(gameInstanceId, matchId, "MATCH_END", Map.of());
    }
    
    private SensorEvent buildEvent(String gameInstanceId, String matchId, String sensorType, Map<String, Object> payload) {
        return SensorEvent.builder()
                .eventId(UUID.randomUUID())
                .gameInstanceId(gameInstanceId)
                .matchId(matchId)
                .sensorType(sensorType)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }
}
