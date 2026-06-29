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
public class EdgeHeartbeatPayload {
    private String edgeId;
    private Instant timestamp;
    private String status; // e.g., ONLINE, OFFLINE
    private Map<String, Object> metrics;
}
