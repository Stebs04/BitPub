package it.uniupo.pissir.bitpub.simulators.freccette;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DartEventGenerator {
    
    public SensorEvent generateDartHitEvent(String gameInstanceId, String matchId, int score, int multiplier) {
        return SensorEvent.builder()
                .eventId(UUID.randomUUID())
                .gameInstanceId(gameInstanceId)
                .matchId(matchId)
                .sensorType("DART_HIT")
                .timestamp(Instant.now())
                .payload(Map.of(
                        "score", score,
                        "multiplier", multiplier
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
