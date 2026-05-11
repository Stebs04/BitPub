package com.bitpub.mqtt;

import com.bitpub.events.SessionForceStoppedEvent;
import com.bitpub.events.SessionStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Componente Spring responsabile dell'ascolto degli eventi applicativi interni
 * e del loro inoltro verso l'infrastruttura Edge tramite il protocollo MQTT.
 * Agisce come ponte tra il dominio dell'applicazione (Service) e il layer di messaggistica.
 */
@Component
public class MqttEventPublisher {

    // Inizializziamo il logger per tracciare le operazioni e facilitare il debug.
    // Utilizziamo SLF4J, che è lo standard nel mondo Spring Boot per il logging.
    private static final Logger logger = LoggerFactory.getLogger(MqttEventPublisher.class);

    @Autowired
    private CloudMqttGateway cloudMqttGateway;

    /**
     * Intercetta l'evento di stop forzato generato dal GameSessionService.
     * * @param event L'evento contenente l'ID del tavolo da fermare.
     */
    @EventListener
    public void onSessionForceStopped(SessionForceStoppedEvent event) {
        logger.info("[MQTT Publisher] Ricevuto evento interno per stop forzato del tavolo ID: {}", event.getTableId());
        try {
            // Richiamiamo il gateway per l'invio effettivo del payload JSON sul topic MQTT dedicato allo stop
            cloudMqttGateway.publishForceStop(event.getTableId());
            logger.debug("[MQTT Publisher] Comando MQTT di stop forzato inoltrato con successo.");
        } catch (Exception e) {
            // Catturiamo l'eccezione per evitare che un errore di rete faccia fallire 
            // la logica principale che ha generato l'evento.
            logger.error("[MQTT Publisher] Impossibile inviare il comando di stop al tavolo {}. Dettaglio errore: {}", event.getTableId(), e.getMessage(), e);
        }
    }

    /**
     * Intercetta l'evento di avvio sessione generato alla creazione di una nuova partita.
     * * @param event L'evento contenente i dettagli della sessione (tavolo e ID partita).
     */
    @EventListener
    public void onSessionStarted(SessionStartedEvent event) {
        logger.info("[MQTT Publisher] Ricevuto evento interno per avvio sessione {} sul tavolo ID: {}", event.getSessionId(), event.getTableId());
        try {
            // Inoltriamo la richiesta al gateway. Il gateway si occuperà di serializzare
            // i dati in JSON e pubblicarli sul topic "bitpub/cloud/foosball/start"
            cloudMqttGateway.publishUnlockBalls(event.getTableId(), event.getSessionId());
            logger.debug("[MQTT Publisher] Comando MQTT di sblocco palline inoltrato con successo.");
        } catch (Exception e) {
            // Gestione isolata dell'errore. La partita rimane salvata nel DB, 
            // ma registriamo nei log che l'impulso hardware non è partito correttamente.
            logger.error("[MQTT Publisher] Impossibile inviare il comando di avvio per la sessione {}. Dettaglio errore: {}", event.getSessionId(), e.getMessage(), e);
        }
    }
}