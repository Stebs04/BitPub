package com.bitpub.mqtt.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class MqttPublisherImpl implements MqttPublisher {

    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, Object payload) {
        publish(topic, payload, 1, false);
    }

    @Override
    public void publish(String topic, Object payload, int qos) {
        publish(topic, payload, qos, false);
    }

    @Override
    public void publish(String topic, Object payload, int qos, boolean retained) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            String traceId = UUID.randomUUID().toString(); // Generate Trace ID for Message Tracing
            
            Message<String> message = MessageBuilder.withPayload(jsonPayload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.QOS, qos)
                    .setHeader(MqttHeaders.RETAINED, retained)
                    .setHeader("traceId", traceId) // User property (MQTTv5) / Spring Message Header
                    .build();

            mqttOutboundChannel.send(message);
            log.debug("Published MQTT message to topic: {}, traceId: {}", topic, traceId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize MQTT payload for topic: {}", topic, e);
            throw new RuntimeException("Failed to serialize MQTT payload", e);
        }
    }
}
