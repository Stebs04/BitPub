package com.bitpub.stats.mqtt;

import com.bitpub.mqtt.payload.GameEventPayload;
import com.bitpub.mqtt.subscriber.AbstractMqttSubscriber;
import com.bitpub.stats.dto.RecordMatchRequest;
import com.bitpub.stats.service.StatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class MatchEndedEventListener extends AbstractMqttSubscriber<GameEventPayload> {

    private final StatsService statsService;

    // Use required=false for mqttOutboundChannel to avoid bean creation issues if not configured globally
    @Autowired
    public MatchEndedEventListener(ObjectMapper objectMapper, 
                                   @Qualifier("mqttOutboundChannel") @Autowired(required = false) MessageChannel mqttOutboundChannel, 
                                   StatsService statsService) {
        super(GameEventPayload.class, objectMapper, mqttOutboundChannel);
        this.statsService = statsService;
    }

    @Override
    protected void processPayload(GameEventPayload payload, String topic, String traceId) {
        if ("ENDED".equalsIgnoreCase(payload.getEventType())) {
            log.info("Ricevuto evento ENDED via MQTT per il gameId: {}, traceId: {}", payload.getGameId(), traceId);
            
            Map<String, Object> data = payload.getEventData();
            if (data == null) {
                log.warn("Nessun eventData trovato nel payload MQTT");
                return;
            }

            try {
                // Estrarre in maniera sicura i dati dal payload
                // Qui usiamo UUID random / default come fallback qualora BitPub-Simulators non invii gli UUID
                String winnerId = String.valueOf(data.getOrDefault("winnerUserId", UUID.randomUUID().toString()));
                String loserId = String.valueOf(data.getOrDefault("loserUserId", UUID.randomUUID().toString()));
                
                // Parsiamo i punteggi in modo robusto (gestendo possibili stringhe)
                int winnerScore = parseScore(data.getOrDefault("winnerScore", "10"));
                int loserScore = parseScore(data.getOrDefault("loserScore", "5"));

                RecordMatchRequest request = RecordMatchRequest.builder()
                        .matchSessionId(UUID.fromString(payload.getEventId() != null ? payload.getEventId() : UUID.randomUUID().toString()))
                        .gameId(UUID.fromString(payload.getGameId()))
                        .winnerUserId(UUID.fromString(winnerId))
                        .loserUserId(UUID.fromString(loserId))
                        .winnerUsername(String.valueOf(data.getOrDefault("winnerUsername", "Vincitore")))
                        .loserUsername(String.valueOf(data.getOrDefault("loserUsername", "Perdente")))
                        .winnerScore(winnerScore)
                        .loserScore(loserScore)
                        .build();

                // Salvataggio effettivo nel database Cloud tramite il servizio esistente
                statsService.recordMatch(request);
                log.info("Partita salvata con successo nel DB per la sessione: {}", request.getMatchSessionId());

            } catch (Exception e) {
                log.error("Errore durante il mapping o il salvataggio della partita da evento MQTT", e);
            }
        }
    }

    private int parseScore(Object scoreObj) {
        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(scoreObj));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Entry point che si aggancia all'Inbound Channel Adapter MQTT configurato nel progetto.
     * Intercetta solo i messaggi dei topic `events/games/+`.
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handle(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        if (topic != null && topic.startsWith("events/games/")) {
            super.handleMessage(message);
        }
    }
}
