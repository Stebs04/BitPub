package it.uniupo.pissir.bitpub.edge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class EventForwardingService {

    private final RuleEngineService ruleEngineService;
    private final MessageChannel cloudMqttOutboundChannel;
    private final ObjectMapper objectMapper;

    public EventForwardingService(RuleEngineService ruleEngineService,
                                  @Qualifier("cloudMqttOutboundChannel") MessageChannel cloudMqttOutboundChannel) {
        this.ruleEngineService = ruleEngineService;
        this.cloudMqttOutboundChannel = cloudMqttOutboundChannel;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Validated sensor events are forwarded Edge -> Cloud over MQTT (QoS1). The broker durably queues
     * them for match-service while it is down, so no app-level buffer is needed; match-service dedups
     * on eventId, so a QoS1 redelivery is safe. This is the only remaining Edge egress — REST fallback
     * and the offline command buffer are gone; the Cloud is reached 100% over MQTT.
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<String> message) {
        String payload = message.getPayload();
        log.info("Received MQTT message on Edge: {}", payload);

        Optional<SensorEvent> optionalEvent = ruleEngineService.validateAndParse(payload);
        if (optionalEvent.isEmpty()) {
            return;
        }

        SensorEvent event = optionalEvent.get();

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize sensor event {}, dropping", event.getEventId(), e);
            return;
        }

        String topic = MqttTopics.getCloudSensorIngestTopic(event.getGameInstanceId());
        cloudMqttOutboundChannel.send(MessageBuilder.withPayload(json)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build());
        log.info("Event {} forwarded to Cloud via MQTT topic {}", event.getEventId(), topic);
    }
}
