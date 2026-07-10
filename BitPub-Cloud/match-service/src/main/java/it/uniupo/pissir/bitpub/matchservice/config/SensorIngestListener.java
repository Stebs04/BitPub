// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.matchservice.service.impl.MatchServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Listener dedicato all'elaborazione degli eventi provenienti dai sensori inoltrati via MQTT.
 * Inserisce i dati nel flusso del {@link MatchServiceImpl#processSensorEvent}, il quale aggiorna
 * i punteggi e notifica i cambiamenti di stato alla WebApp.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SensorIngestListener {

    private final MatchServiceImpl matchService;
    private final ObjectMapper objectMapper;

    // Intercetta i messaggi in arrivo sul canale di ingresso MQTT relativo ai sensori
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void onSensorEvent(Message<String> message) {
        try {
            SensorEvent event = objectMapper.readValue(message.getPayload(), SensorEvent.class);
            log.info("Evento sensore ricevuto via MQTT: tipo={}, partita={}",
                    event.getSensorType(), event.getGameInstanceId());
            matchService.processSensorEvent(event);
        } catch (Exception e) {
            // Un'eccezione viene intercettata senza bloccare il flusso per garantire l'elaborazione dei messaggi successivi
            log.error("Errore durante l'elaborazione dell'evento sensore: {}", message.getPayload(), e);
        }
    }
}
