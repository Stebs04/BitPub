package com.bitpub.mqtt.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusPayload {
    private String serviceName;
    private String status; // e.g., UP, DOWN, MAINTENANCE
    private Instant timestamp;
    private String version;
}
