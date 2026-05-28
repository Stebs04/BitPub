package com.bitpub.mqtt.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameEventPayload {
    private String eventId;
    private String gameId;
    private String localeId;
    private String eventType; // e.g., STARTED, PAUSED, ENDED, GOAL
    private Map<String, Object> eventData;
    private Instant timestamp;
}
