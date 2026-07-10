/**
 * Autore: Stefano Bellan Matricola 20054330
 */
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
 * Listener MQTT dedicato al consumo dei risultati delle partite concluse, inviati dal match-service.
 * Questi dati vengono successivamente demandati al servizio {@link StatisticsService#recordMatchResult} per l'elaborazione.
 * Questo approccio asincrono via MQTT sostituisce la precedente chiamata sincrona REST.
 * L'inserimento dei dati è progettato per essere idempotente basandosi sull'identificativo della partita: 
 * in questo modo eventuali messaggi riconsegnati (QoS 1 con sessione durevole) non causeranno conteggi duplicati.
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
            // Gestione dei messaggi non validi: l'eccezione viene intercettata e soppressa per non bloccare la coda durevole. L'idempotenza copre i tentativi di riconsegna successivi.
            log.error("Failed to ingest match result: {}", message.getPayload(), e);
        }
    }
}
