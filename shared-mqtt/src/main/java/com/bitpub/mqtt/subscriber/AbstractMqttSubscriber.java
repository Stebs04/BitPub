package com.bitpub.mqtt.subscriber;

import com.bitpub.mqtt.registry.MqttTopicRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

/**
 * Base abstraction for MQTT Subscribers.
 * Handles deserialization and routing to DLT on failure.
 *
 * @param <T> The payload type
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractMqttSubscriber<T> {

    private final Class<T> payloadType;
    private final ObjectMapper objectMapper;
    private final MessageChannel mqttOutboundChannel;

    /**
     * Process the decoded payload.
     */
    protected abstract void processPayload(T payload, String topic, String traceId);

    /**
     * Handles the incoming raw MQTT message.
     */
    public void handleMessage(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String traceId = message.getHeaders().get("traceId", String.class);
        
        log.debug("Received message on topic: {}, traceId: {}", topic, traceId);

        try {
            String rawPayload = extractPayload(message);
            T payload = objectMapper.readValue(rawPayload, payloadType);
            
            processPayload(payload, topic, traceId);
            
        } catch (Exception e) {
            log.error("Error processing message on topic: {}. Routing to DLT.", topic, e);
            routeToDlt(message, topic, e.getMessage());
        }
    }

    private String extractPayload(Message<?> message) {
        Object payload = message.getPayload();
        if (payload instanceof byte[]) {
            return new String((byte[]) payload, StandardCharsets.UTF_8);
        } else if (payload instanceof String) {
            return (String) payload;
        }
        throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass());
    }

    private void routeToDlt(Message<?> failedMessage, String originalTopic, String errorMessage) {
        if (mqttOutboundChannel == null || originalTopic == null) return;
        
        String dltTopic = MqttTopicRegistry.dlt(originalTopic);
        
        Message<?> dltMessage = MessageBuilder.fromMessage(failedMessage)
                .setHeader(MqttHeaders.TOPIC, dltTopic)
                .setHeader("errorCause", errorMessage)
                .build();
                
        try {
            mqttOutboundChannel.send(dltMessage);
            log.info("Message routed to DLT: {}", dltTopic);
        } catch (Exception e) {
            log.error("Failed to route message to DLT: {}", dltTopic, e);
        }
    }
}
