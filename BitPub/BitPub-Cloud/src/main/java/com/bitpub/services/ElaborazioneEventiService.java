package com.bitpub.cloud.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// Immaginiamo che Timothy abbia creato questo DAO per il database
// import com.bitpub.cloud.repository.EventoBiliardoRepository;

@Service
public class ElaborazioneEventiService {

    // Qui inietteremo le interfacce Spring Data JPA fatte da Timothy
    // @Autowired
    // private EventoBiliardoRepository biliardoRepo;

    /**
     * Metodo asincrono: viene eseguito in un thread separato dal Thread Pool.
     * Non blocca il client MQTT!
     */
    @Async("mqttDbTaskExecutor")
    public void processaESalvaEvento(String topic, String payloadJson) {
        try {
            // 1. Capiamo da quale gioco arriva (usando l'albero dei topic che hai definito prima)
            System.out.println("[" + Thread.currentThread().getName() + "] Inizio salvataggio evento da topic: " + topic);

            if (topic.contains("biliardo")) {
                // TODO: Usare GSON per convertire il payloadJson nell'Entity creata da Timothy
                // EventoBiliardoEntity entity = gson.fromJson(payloadJson, EventoBiliardoEntity.class);

                // 2. Salvataggio nel database tramite il DAO di Timothy
                // biliardoRepo.save(entity);
                System.out.println("[" + Thread.currentThread().getName() + "] Evento Biliardo salvato su PostgreSQL!");
            }
            // Aggiungerete poi gli if per calciobalilla e freccette

        } catch (Exception e) {
            System.err.println("Errore durante il salvataggio asincrono a DB: " + e.getMessage());
        }
    }
}