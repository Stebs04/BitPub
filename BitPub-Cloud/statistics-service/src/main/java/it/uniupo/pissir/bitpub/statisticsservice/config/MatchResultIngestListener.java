package it.uniupo.pissir.bitpub.statisticsservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.statisticsservice.dto.MatchResultEvent;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Consuma i risultati dei match conclusi inoltrati dal match-service via MQTT
 * (bitpub/cloud/matches/result) e li passa a {@link StatisticsService#recordMatchResult}.
 * Sostituisce la vecchia POST REST /api/v1/statistics/match-result. L'ingest e' idempotente
 * sul matchId, quindi una riconsegna QoS1 (sessione durevole) non raddoppia i conteggi.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchResultIngestListener {

    private final StatisticsService statisticsService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttResultInboundChannel")
    public void onMatchResult(Message<String> message) {
        try {
            MatchResultEvent event = objectMapper.readValue(message.getPayload(), MatchResultEvent.class);
            statisticsService.recordMatchResult(event);
            log.info("Ingested match result via MQTT: match={}, winner={}", event.getMatchId(), event.getWinnerName());
        } catch (Exception e) {
            // Poison message: swallow so it doesn't wedge the durable queue; idempotency covers replays.
            log.error("Failed to ingest match result: {}", message.getPayload(), e);
        }
    }
}
