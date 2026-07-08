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

        // Stato live autoritativo sull'Edge: aggiorna turno + punteggio e pubblica SUBITO il nuovo
        // stato sul broker locale (topic match-state) cosi' il frontend dell'altro giocatore sblocca
        // il turno all'istante. A fine partita riporta i punteggi finali al Cloud per la persistenza.
        ruleEngineService.applyEvent(event).ifPresent(state -> {
            publishLocalState(state, event.getSensorType());
            if (state.finished) {
                ruleEngineService.reportResultToCloud(state);
            }
        });

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

    /**
     * Pubblica lo stato live sul broker locale (topic match-state), con RETAINED cosi' un subscriber
     * tardivo/riconnesso riceve subito l'ultimo stato. ponytail: qui il broker locale e quello cloud
     * sono la stessa mosquitto, quindi si riusa cloudMqttOutboundChannel; separarli solo se le due
     * istanze verranno davvero divise.
     */
    private void publishLocalState(RuleEngineService.LocalMatchState state, String eventMessage) {
        try {
            String payload = objectMapper.writeValueAsString(ruleEngineService.buildStatePayload(state, eventMessage));
            String topic = MqttTopics.getGameStateTopic(
                    state.localeId != null ? state.localeId : "unknown", state.gameInstanceId);
            cloudMqttOutboundChannel.send(MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.RETAINED, true)
                    .build());
            log.info("Edge published live state for match {} (turn={}) to {}",
                    state.matchId, state.currentTurnUserId, topic);
        } catch (Exception e) {
            log.error("Edge failed to publish live state for match {}", state.matchId, e);
        }
    }
}
