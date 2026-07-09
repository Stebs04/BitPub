package it.uniupo.pissir.bitpub.edge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class EventForwardingService {

    private final RuleEngineService ruleEngineService;
    private final MqttBufferService mqttBuffer;
    private final ObjectMapper objectMapper;

    public EventForwardingService(RuleEngineService ruleEngineService, MqttBufferService mqttBuffer) {
        this.ruleEngineService = ruleEngineService;
        this.mqttBuffer = mqttBuffer;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Cloud -> Edge full match state push (topic bitpub/edge/matches/+/sync). Eagerly initializes the
     * authoritative LocalMatchState so the Edge no longer pulls the roster via REST and keeps running
     * (turn gate + live scoring) even while the Cloud is unreachable.
     */
    @ServiceActivator(inputChannel = "matchSyncInputChannel")
    public void handleMatchSync(Message<String> message) {
        try {
            JsonNode m = objectMapper.readTree(message.getPayload());
            ruleEngineService.initFromSync(m);
        } catch (Exception e) {
            log.error("Edge failed to parse match sync payload: {}", message.getPayload(), e);
        }
    }

    /**
     * Validated sensor events are forwarded Edge -> Cloud over MQTT (QoS1) through the explicit offline
     * buffer, which queues them locally while the Cloud connection is DOWN and flushes on reconnect.
     * At match end the enriched final result (players, exact scores, winner) is published the same way,
     * so a result produced during a cloud outage is buffered instead of lost.
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
        // il turno all'istante. A fine partita riporta l'esito finale arricchito al Cloud (via buffer).
        ruleEngineService.applyEvent(event).ifPresent(state -> {
            publishLocalState(state, event.getSensorType());
            if (state.finished) {
                reportResult(state);
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
        mqttBuffer.send(MessageBuilder.withPayload(json)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build(), "sensor event " + event.getEventId());
        log.info("Event {} forwarded to Cloud via MQTT topic {}", event.getEventId(), topic);
    }

    /**
     * A fine partita pubblica l'esito finale arricchito (giocatori, punteggi esatti, vincitore) sul
     * topic edge-match-result via il buffer offline, poi libera lo stato live. Sostituisce la vecchia
     * POST REST /result: se il Cloud e' giu' l'esito viene bufferizzato e riconsegnato al ripristino.
     */
    private void reportResult(RuleEngineService.LocalMatchState state) {
        try {
            String payload = objectMapper.writeValueAsString(ruleEngineService.buildResultPayload(state));
            String topic = MqttTopics.getCloudMatchResultTopic(state.matchId);
            mqttBuffer.send(MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build(), "result for Match " + state.matchId);
            log.info("Edge reported final result for match {} to cloud topic {}", state.matchId, topic);
        } catch (Exception e) {
            log.error("Edge failed to serialize final result for match {}", state.matchId, e);
        }
        ruleEngineService.clearState(state.matchId);
    }

    /**
     * Pubblica lo stato live sul broker locale (topic match-state), con RETAINED cosi' un subscriber
     * tardivo/riconnesso riceve subito l'ultimo stato. ponytail: qui il broker locale e quello cloud
     * sono la stessa mosquitto, quindi si riusa il buffer/canale cloud; separarli solo se le due
     * istanze verranno davvero divise.
     */
    private void publishLocalState(RuleEngineService.LocalMatchState state, String eventMessage) {
        try {
            String payload = objectMapper.writeValueAsString(ruleEngineService.buildStatePayload(state, eventMessage));
            String topic = MqttTopics.getGameStateTopic(
                    state.localeId != null ? state.localeId : "unknown", state.gameInstanceId);
            mqttBuffer.send(MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.RETAINED, true)
                    .build(), "live state for Match " + state.matchId);
            log.info("Edge published live state for match {} (turn={}) to {}",
                    state.matchId, state.currentTurnUserId, topic);
        } catch (Exception e) {
            log.error("Edge failed to publish live state for match {}", state.matchId, e);
        }
    }
}
