package com.bitpub.cloud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// import com.bitpub.cloud.repository.EventoBiliardoRepository;

@Service
public class ElaborazioneEventiService {

    // 1. Inizializziamo il Logger specifico per questo Service!
    private static final Logger log = LoggerFactory.getLogger(ElaborazioneEventiService.class);

    // Interfacce Spring Data JPA
    // @Autowired
    // private EventoBiliardoRepository biliardoRepo;

    /**
     * Metodo asincrono: viene eseguito in un thread separato dal Thread Pool.
     * Non blocca il client MQTT!
     */
    @Async("mqttDbTaskExecutor")
    public void processaESalvaEvento(String topic, String payloadJson) {

        log.info("Inizio elaborazione evento MQTT. Topic di origine: {}", topic);

        try {
            // 1. Capiamo da quale gioco arriva
            if (topic.contains("biliardo")) {
                log.debug("Rilevato evento Biliardo. Avvio conversione JSON...");

                // TODO: Usare GSON per convertire il payloadJson nell'Entity creata da Timothy
                // EventoBiliardoEntity entity = gson.fromJson(payloadJson, EventoBiliardoEntity.class);

                // 2. Salvataggio nel database tramite il DAO di Timothy
                // biliardoRepo.save(entity);

                log.info("Evento Biliardo salvato con successo su PostgreSQL!");
            }
            // Aggiungerete poi gli if per calciobalilla e freccette
            else if (topic.contains("freccette")) {
                log.debug("Rilevato evento Freccette. (Logica in costruzione...)");
            }

        } catch (Exception e) {
            // Se la conversione JSON fallisce o il database va offline, lo catturiamo qui
            log.error("ERRORE durante l'elaborazione o il salvataggio dell'evento dal topic '{}'. Motivo: {}", topic, e.getMessage(), e);
        }
    }
}