package com.bitpub.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricsService {

    private final MeterRegistry registry;
    private final AtomicInteger activeEdges;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
        
        // Gauge for active edges
        this.activeEdges = registry.gauge("active.edges", new AtomicInteger(0));
    }

    public void incrementAuthFailures() {
        Counter.builder("auth.failures")
                .description("Number of authentication failures")
                .register(registry)
                .increment();
    }

    public void incrementSyncFailures() {
        Counter.builder("sync.failures")
                .description("Number of sync failures")
                .register(registry)
                .increment();
    }
    
    public void incrementMatchesCreated() {
        Counter.builder("matches.created")
                .description("Number of matches created (can be used for matches/minute)")
                .register(registry)
                .increment();
    }
    
    public Timer getMqttLatencyTimer() {
        return Timer.builder("mqtt.latency")
                .description("Latency of MQTT message processing")
                .register(registry);
    }
    
    public void setActiveEdges(int count) {
        if (activeEdges != null) {
            activeEdges.set(count);
        }
    }
    
    public void incrementActiveEdges() {
        if (activeEdges != null) {
            activeEdges.incrementAndGet();
        }
    }
    
    public void decrementActiveEdges() {
        if (activeEdges != null) {
            activeEdges.decrementAndGet();
        }
    }
}
