package it.uniupo.pissir.bitpub.simulators.calciobalilla;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class FoosballEventGenerator {
    
    public SensorEvent generateMatchStartEvent(String gameInstanceId, String matchId) {
        return buildEvent(gameInstanceId, matchId, "MATCH_START", Map.of());
    }

    public SensorEvent generateMatchEndEvent(String gameInstanceId, String matchId) {
        return buildEvent(gameInstanceId, matchId, "MATCH_END", Map.of());
    }

    public SensorEvent generateGoalEvent(String gameInstanceId, String matchId, String team) {
        return buildEvent(gameInstanceId, matchId, "GOAL", Map.of("team", team));
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
