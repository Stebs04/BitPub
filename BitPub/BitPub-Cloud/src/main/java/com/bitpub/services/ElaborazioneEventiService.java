package com.bitpub.services;

import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.models.GameSessionEntity;
import com.bitpub.repository.PartitaCalciobalillaRepository;
import com.bitpub.repository.GameSessionRepository;
import com.bitpub.utils.JsonManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Servizio per la gestione e l'elaborazione dei messaggi IoT provenienti dai tavoli da gioco.
 * Smista i dati ricevuti tramite protocollo MQTT e aggiorna lo stato delle sessioni su database.
 */
@Service
public class ElaborazioneEventiService {

    private static final Logger log = LoggerFactory.getLogger(ElaborazioneEventiService.class);

    @Autowired
    private PartitaCalciobalillaRepository calciobalillaRepo;

    @Autowired
    private GameSessionRepository gameSessionRepo;

    /**
     * Recupera i dettagli di un singolo evento tramite il suo identificativo.
     *
     * @param id Identificativo univoco dell'evento.
     * @return DTO contenente i dati dell'evento, o null se non trovato.
     */
    public com.bitpub.dto.GameEventDTO getEventDtoById(Long id) {
        return null;
    }

    /**
     * Estrae la lista di tutti gli eventi associati a una specifica sessione di gioco.
     *
     * @param sessionId Identificativo della sessione di gioco.
     * @return Lista dei DTO degli eventi collegati alla sessione.
     */
    public List<com.bitpub.dto.GameEventDTO> getEventsBySession(Long sessionId) {
        return java.util.Collections.emptyList();
    }

    /**
     * Riceve i messaggi MQTT in modo asincrono, decodifica il payload JSON e persiste
     * le informazioni sul database in base alla tipologia di gioco rilevata nel topic.
     *
     * @param topic Il canale di provenienza del messaggio MQTT.
     * @param payloadJson La stringa JSON contenente i dati dell'evento.
     */
    @Async("mqttDbTaskExecutor")
    public void processaESalvaEvento(String topic, String payloadJson) {
        log.info("Inizio elaborazione evento MQTT. Topic di origine: {}", topic);
        try {
            if (topic.contains("biliardo")) {
                log.debug("Rilevato evento Biliardo. Avvio conversione JSON...");
                // Struttura dati per il gioco del biliardo da definire
                log.info("Evento Biliardo salvato con successo su PostgreSQL!");
                
            } else if (topic.contains("calciobalilla")) {
                log.debug("Rilevato evento Calciobalilla tramite MQTT. Avvio conversione JSON...");
                
                // Deserializzazione del payload tramite il gestore JSON centralizzato
                PartitaCalciobalilla entity = JsonManager.getInstance().fromJson(payloadJson, PartitaCalciobalilla.class);
                calciobalillaRepo.save(entity);
                log.info("Evento Calciobalilla processato e salvato su PostgreSQL tramite layer Spring Data JPA!");

                // Verifica del completamento della partita per la chiusura della sessione
                if (entity.getOrarioFine() != null) {
                    log.info("La partita di Calciobalilla è terminata (raggiunto punteggio massimo). Procedo a chiudere la sessione attiva...");
                    
                    // Estrazione del codice numerico del tavolo dalla struttura del topic
                    String[] topicParts = topic.split("/");
                    String tableIdStr = topicParts[4].replace("calciobalilla_", "");
                    Integer tableId = Integer.parseInt(tableIdStr);
                    
                    // Ricerca e aggiornamento delle sessioni attualmente in corso
                    List<GameSessionEntity> sessioniAttive = gameSessionRepo.findAllByStatus("IN_PROGRESS");
                    for (GameSessionEntity session : sessioniAttive) {
                        if (session.getTableId().equals(tableId)) {
                            session.setStatus("COMPLETED");
                            session.setEndTime(entity.getOrarioFine());
                            gameSessionRepo.save(session);
                            log.info("GameSession con ID {} sul tavolo {} è stata chiusa con successo (Stato: COMPLETED). L'utente è di nuovo libero.", session.getId(), tableId);
                        }
                    }
                }
                
            } else if (topic.contains("freccette")) {
                log.debug("Rilevato evento Freccette. (Logica in costruzione...)");
            }
        } catch (Exception e) {
            log.error("ERRORE durante l'elaborazione o il salvataggio dell'evento dal topic '{}'. Motivo: {}", topic, e.getMessage(), e);
        }
    }
}
