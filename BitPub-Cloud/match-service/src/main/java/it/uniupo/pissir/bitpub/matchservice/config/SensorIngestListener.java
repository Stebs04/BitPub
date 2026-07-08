package it.uniupo.pissir.bitpub.matchservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.matchservice.service.impl.MatchServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Consumes Edge-forwarded sensor events from the Cloud ingest MQTT topic and feeds them into the
 * existing {@link MatchServiceImpl#processSensorEvent} pipeline (which updates scores and republishes
 * the game state to the WebApp topic). Replaces the former REST POST /api/matches/events ingress.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SensorIngestListener {

    private final MatchServiceImpl matchService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void onSensorEvent(Message<String> message) {
        try {
            SensorEvent event = objectMapper.readValue(message.getPayload(), SensorEvent.class);
            log.info("Received sensor event via MQTT: type={}, gameInstanceId={}",
                    event.getSensorType(), event.getGameInstanceId());
            matchService.processSensorEvent(event);
        } catch (Exception e) {
            // Swallow so a poison message doesn't wedge the QoS1 queue; idempotency covers replays.
            log.error("Failed to process inbound sensor event: {}", message.getPayload(), e);
        }
    }
}
