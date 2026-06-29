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
public class GlobalStatsPayload {
    private Instant timestamp;
    private long totalActiveUsers;
    private long totalActiveGames;
    private Map<String, Object> additionalMetrics;
}
