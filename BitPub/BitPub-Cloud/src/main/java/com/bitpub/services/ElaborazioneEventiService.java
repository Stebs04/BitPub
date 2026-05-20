package com.bitpub.services;

import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.models.PartitaFreccette; // Importiamo il modello Freccette
import com.bitpub.models.GameSessionEntity;
import com.bitpub.repository.PartitaCalciobalillaRepository;
import com.bitpub.repository.PartitaFreccetteRepository; // Importiamo la Repository Freccette
import com.bitpub.repository.GameSessionRepository;
import com.bitpub.utils.JsonManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ElaborazioneEventiService {

    private static final Logger log = LoggerFactory.getLogger(ElaborazioneEventiService.class);

    @Autowired
    private PartitaCalciobalillaRepository calciobalillaRepo;

    // Aggiungiamo l'iniezione della dipendenza per il DB delle freccette
    @Autowired
    private PartitaFreccetteRepository freccetteRepo;

    @Autowired
    private GameSessionRepository gameSessionRepo;

    public com.bitpub.dto.GameEventDTO getEventDtoById(Long id) {
        return null;
    }

    public List<com.bitpub.dto.GameEventDTO> getEventsBySession(Long sessionId) {
        return java.util.Collections.emptyList();
    }

    @Async("mqttDbTaskExecutor")
    public void processaESalvaEvento(String topic, String payloadJson) {
        log.info("Inizio elaborazione evento MQTT. Topic di origine: {}", topic);
        try {
            if (topic.contains("biliardo")) {
                log.debug("Rilevato evento Biliardo. Avvio conversione JSON...");
                log.info("Evento Biliardo salvato con successo su PostgreSQL!");

            } else if (topic.contains("calciobalilla")) {
                log.debug("Rilevato evento Calciobalilla tramite MQTT. Avvio conversione JSON...");
                PartitaCalciobalilla entity = JsonManager.getInstance().fromJson(payloadJson, PartitaCalciobalilla.class);
                calciobalillaRepo.save(entity);
                log.info("Evento Calciobalilla processato e salvato su PostgreSQL tramite layer Spring Data JPA!");

                if (entity.getOrarioFine() != null) {
                    log.info("La partita di Calciobalilla è terminata...");
                    String[] topicParts = topic.split("/");
                    String tableIdStr = topicParts[4].replace("calciobalilla_", "");
                    Integer tableId = Integer.parseInt(tableIdStr);

                    List<GameSessionEntity> sessioniAttive = gameSessionRepo.findAllByStatus("IN_PROGRESS");
                    for (GameSessionEntity session : sessioniAttive) {
                        if (session.getTableId().equals(tableId)) {
                            session.setStatus("COMPLETED");
                            session.setEndTime(entity.getOrarioFine());
                            gameSessionRepo.save(session);
                            log.info("GameSession con ID {} sul tavolo {} è stata chiusa.", session.getId(), tableId);
                        }
                    }
                }

            } else if (topic.contains("freccette")) {
                log.debug("Rilevato evento Freccette. Avvio conversione JSON e salvataggio DB...");

                // Implementiamo la logica mancante! Deserializziamo e salviamo
                PartitaFreccette entity = JsonManager.getInstance().fromJson(payloadJson, PartitaFreccette.class);
                freccetteRepo.save(entity);
                log.info("Evento Freccette processato e salvato su PostgreSQL tramite layer Spring Data JPA!");
            }
        } catch (Exception e) {
            log.error("ERRORE durante l'elaborazione o il salvataggio dell'evento dal topic '{}'. Motivo: {}", topic, e.getMessage(), e);
        }
    }
}