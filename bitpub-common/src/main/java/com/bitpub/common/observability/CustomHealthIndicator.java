package com.bitpub.common.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("customHealthIndicator")
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Implement custom health check logic here
        // For example, checking a specific directory, memory limit, or a critical dependency
        boolean isHealthy = checkCustomCondition();

        if (isHealthy) {
            return Health.up()
                    .withDetail("status", "System is operating normally")
                    .build();
        }
        
        return Health.down()
                .withDetail("status", "System detected an anomaly")
                .withDetail("error", "Custom condition failed")
                .build();
    }

    private boolean checkCustomCondition() {
        // Placeholder for real logic
        return true; 
    }
}
