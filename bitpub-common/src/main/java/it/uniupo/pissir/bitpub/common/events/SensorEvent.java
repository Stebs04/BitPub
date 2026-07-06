package it.uniupo.pissir.bitpub.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorEvent {
    /**
     * Unique ID for the event, used for idempotent deduplication.
     */
    private UUID eventId;
    
    /**
     * ID of the installed game instance (locale-scoped).
     */
    private String gameInstanceId;
    
    /**
     * ID of the match, if the event is tied to an ongoing match.
     */
    private String matchId;
    
    /**
     * Type of sensor event (e.g. GOAL, MATCH_START, BALL_POCKETED, DART_HIT)
     */
    private String sensorType;
    
    /**
     * Timestamp of when the event actually occurred at the edge.
     */
    private Instant timestamp;
    
    /**
     * Additional payload specific to the sensor type.
     * E.g., for DART_HIT: {"score": 20, "multiplier": 3}
     * E.g., for BALL_POCKETED: {"pocketId": 3, "ballNumber": 8, "ballColor": "BLACK"}
     */
    private Map<String, Object> payload;

    /** Null-guard: generate an id lazily so an event without one is still deduplicatable. */
    public UUID getEventId() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        return eventId;
    }
}
