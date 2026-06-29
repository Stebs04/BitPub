package com.bitpub.simulator.service;

import com.bitpub.mqtt.publisher.MqttPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SimulatorService {

    private final MqttPublisher mqttPublisher;

    public SimulatorService(MqttPublisher mqttPublisher) {
        this.mqttPublisher = mqttPublisher;
    }

    public void simulateCalciobalilla() {
        Map<String, String> payload = Map.of("gameType", "TABLE_FOOTBALL", "action", "START");
        mqttPublisher.publish("cmd/simulators/start", payload);
    }

    public void simulateBiliardo() {
        Map<String, String> payload = Map.of("gameType", "POOL", "action", "START");
        mqttPublisher.publish("cmd/simulators/start", payload);
    }

    public void simulateFreccette() {
        Map<String, String> payload = Map.of("gameType", "DARTS", "action", "START");
        mqttPublisher.publish("cmd/simulators/start", payload);
    }
}
