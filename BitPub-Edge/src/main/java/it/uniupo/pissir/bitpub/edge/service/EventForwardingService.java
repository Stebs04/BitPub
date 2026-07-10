/**
 * Autore: Timothy Giolito 20054431
 */
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
     * Ricezione dello stato partita completo dal Cloud.
     * Inizializza subito lo stato locale (LocalMatchState) in modo che l'Edge possa gestire in autonomia
     * il calcolo del punteggio e il cambio dei turni, senza fare chiamate REST e garantendo il funzionamento
     * anche a Cloud spento.
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
     * Inoltra gli eventi dei sensori validati verso il Cloud tramite MQTT (QoS1).
     * Sfrutta il buffer offline per non perdere i messaggi quando la connessione cade.
     * A fine partita prepariamo un resoconto arricchito con punteggi e vincitore, e lo pubblichiamo
     * usando lo stesso canale per assicurarci che non vada perso.
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

        // Manteniamo lo stato sul nodo Edge. Aggiorniamo turno e punteggio e li pubblichiamo
        // immediatamente sul broker locale, permettendo ai frontend di aggiornarsi senza ritardi.
        // A fine partita ci occupiamo di preparare l'esito finale per il Cloud.
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
     * Pubblica l'esito conclusivo della partita includendo giocatori, punteggi e vincitore.
     * Utilizza MQTT con il buffer offline invece della vecchia chiamata REST, in modo
     * da accodare l'esito qualora il Cloud risulti offline al momento della fine.
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
     * Rende disponibile lo stato della partita sul broker locale usando l'opzione RETAINED,
     * consentendo a eventuali client disconnessi di riallinearsi istantaneamente.
     * In questa demo usiamo la stessa istanza Mosquitto sia per locale che per cloud, riutilizzando
     * così il canale del buffer.
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
