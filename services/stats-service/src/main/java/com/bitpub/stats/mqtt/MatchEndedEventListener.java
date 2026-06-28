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
public class MatchEndedEventListener extends AbstractMqttSubscriber<com.fasterxml.jackson.databind.JsonNode> {

    private final StatsService statsService;

    // Use required=false for mqttOutboundChannel to avoid bean creation issues if not configured globally
    @Autowired
    public MatchEndedEventListener(ObjectMapper objectMapper, 
                                   @Qualifier("mqttOutboundChannel") @Autowired(required = false) MessageChannel mqttOutboundChannel, 
                                   StatsService statsService) {
        super(com.fasterxml.jackson.databind.JsonNode.class, objectMapper, mqttOutboundChannel);
        this.statsService = statsService;
    }

    @Override
    protected void processPayload(com.fasterxml.jackson.databind.JsonNode payload, String topic, String traceId) {
        // Handle both old GameEventPayload (eventType=ENDED) and new MatchEndedEvent (type/eventType=MATCH_ENDED)
        String eventType = payload.has("eventType") ? payload.get("eventType").asText() : null;
        String type = payload.has("type") ? payload.get("type").asText() : null;
        
        if ("ENDED".equalsIgnoreCase(eventType) || "MATCH_ENDED".equalsIgnoreCase(eventType) || "MATCH_ENDED".equalsIgnoreCase(type)) {
            log.info("Ricevuto evento ENDED via MQTT per il traceId: {}", traceId);
            
            try {
                com.fasterxml.jackson.databind.JsonNode data = payload.has("eventData") ? payload.get("eventData") : payload;
                String gameId = payload.has("gameId") ? payload.get("gameId").asText() : UUID.randomUUID().toString();
                String eventId = payload.has("eventId") ? payload.get("eventId").asText() : UUID.randomUUID().toString();
                
                String winnerId = data.has("winnerUserId") ? data.get("winnerUserId").asText() : UUID.randomUUID().toString();
                String loserId = data.has("loserUserId") ? data.get("loserUserId").asText() : UUID.randomUUID().toString();
                
                // Fallback for Simulator's MatchEndedEvent which uses winningTeam and finalScoreTeamA/B
                if (data.has("winningTeam")) {
                    String winningTeam = data.get("winningTeam").asText();
                    int scoreA = data.has("finalScoreTeamA") ? data.get("finalScoreTeamA").asInt() : 0;
                    int scoreB = data.has("finalScoreTeamB") ? data.get("finalScoreTeamB").asInt() : 0;
                    
                    winnerId = "TEAM_A".equals(winningTeam) ? "player-teamA-123" : "player-teamB-456";
                    loserId = "TEAM_A".equals(winningTeam) ? "player-teamB-456" : "player-teamA-123";
                    
                    RecordMatchRequest request = RecordMatchRequest.builder()
                            .matchSessionId(UUID.fromString(eventId))
                            .gameId(UUID.randomUUID()) // placeholder since gameId is string in event but UUID in DB
                            .winnerUserId(UUID.randomUUID())
                            .loserUserId(UUID.randomUUID())
                            .winnerUsername("Team Vincitore (" + winningTeam + ")")
                            .loserUsername("Team Perdente")
                            .winnerScore(Math.max(scoreA, scoreB))
                            .loserScore(Math.min(scoreA, scoreB))
                            .build();
                    statsService.recordMatch(request);
                    log.info("Partita simulata salvata con successo nel DB per la sessione: {}", request.getMatchSessionId());
                    return;
                }

                int winnerScore = parseScore(data.has("winnerScore") ? data.get("winnerScore").asText() : "10");
                int loserScore = parseScore(data.has("loserScore") ? data.get("loserScore").asText() : "5");

                RecordMatchRequest request = RecordMatchRequest.builder()
                        .matchSessionId(UUID.fromString(eventId))
                        .gameId(UUID.fromString(gameId))
                        .winnerUserId(UUID.fromString(winnerId))
                        .loserUserId(UUID.fromString(loserId))
                        .winnerUsername(data.has("winnerUsername") ? data.get("winnerUsername").asText() : "Vincitore")
                        .loserUsername(data.has("loserUsername") ? data.get("loserUsername").asText() : "Perdente")
                        .winnerScore(winnerScore)
                        .loserScore(loserScore)
                        .build();

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
        if (topic != null && topic.startsWith("v1/games/")) {
            super.handleMessage(message);
        }
    }
}
