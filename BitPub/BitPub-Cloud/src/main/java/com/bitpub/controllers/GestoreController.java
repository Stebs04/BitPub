package com.bitpub.controllers;

import com.bitpub.models.Torneo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST per la gestione delle operazioni del Gestore del locale lato Backend.
 * <p>
 * Espone le API utilizzate dal client JavaFX per il polling dello stato delle macchine,
 * il monitoraggio delle partite e la creazione dei tornei.
 * </p>
 *
 * @author Stefano Bellan
 * @version 1.0
 */
@RestController
@RequestMapping("/gestore")
public class GestoreController {

    /**
     * Recupera i dati per il monitoraggio real-time della dashboard del gestore.
     *
     * @return DTO formattato come Map contenente macchine, partite e statistiche.
     */
    @GetMapping("/monitoraggio")
    public ResponseEntity<Object> getMonitoraggio() {
        // TODO: Implementare il recupero dei dati reali dal DB
        return ResponseEntity.ok(Map.of(
            "macchine", new Object[0],
            "partiteRecenti", new Object[0],
            "statistiche", new Object[0]
        ));
    }

    /**
     * Inserisce e programma un nuovo torneo a sistema.
     *
     * @param nuovoTorneo L'oggetto Torneo ricevuto in formato JSON dal client.
     * @return Risposta testuale di conferma.
     */
    @PostMapping("/tornei")
    public ResponseEntity<String> creaTorneo(@RequestBody Torneo nuovoTorneo) {
        // TODO: Salvare il torneo utilizzando il TorneoRepository.
        return ResponseEntity.ok("Torneo creato con successo nel backend.");
    }
}