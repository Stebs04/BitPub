package com.bitpub.contracts.events;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class EdgeHeartbeatEvent extends BaseSensorEvent {
    
    @NotBlank
    private String edgeNodeId;
    
    private String status; // e.g., "ONLINE", "OFFLINE", "DEGRADED"
    
    private double cpuUsage;
    
    private double memoryUsage;
}
