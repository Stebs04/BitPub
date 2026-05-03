package com.bitpub.controllers;

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

    // Aggiungiamo l'endpoint che il client JavaFX cerca per le macchine
    @GetMapping("/locali/{id}/macchine")
    public ResponseEntity<?> getMacchineLocale(@PathVariable Long id) {
        // Ritorna un array vuoto in formato JSON per evitare il crash del client
        return ResponseEntity.ok(new String[]{});
    }

    // Aggiungiamo l'endpoint che il client JavaFX cerca per le partite attive
    @GetMapping("/locali/{id}/partite/attive")
    public ResponseEntity<?> getPartiteAttive(@PathVariable Long id) {
        // Ritorna un array vuoto in formato JSON per evitare il crash del client
        return ResponseEntity.ok(new Object[]{});
    }

    @GetMapping("/monitoraggio")
    public ResponseEntity<Object> getMonitoraggio() {
        return ResponseEntity.ok(Map.of(
                "macchine", new Object[0],
                "partiteRecenti", new Object[0],
                "statistiche", new Object[0]
        ));
    }
}