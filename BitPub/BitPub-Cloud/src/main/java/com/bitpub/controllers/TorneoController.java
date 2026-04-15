package com.bitpub.controllers;

import com.bitpub.models.Torneo;
import com.bitpub.repository.TorneoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller REST per la gestione dei Tornei.
 * Architettura rigorosamente State-Less.
 */
@RestController
@RequestMapping("/api/tornei")
public class TorneoController {

    // 1. Colleghiamo il Controller al Database!
    @Autowired
    private TorneoRepository torneoRepository;

    /**
     * POST: Crea e salva un nuovo torneo nel database.
     */
    @PostMapping
    public ResponseEntity<String> creaTorneo(
            @RequestBody Torneo nuovoTorneo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        System.out.println("[API REST] Richiesta creazione torneo. Token: " + authHeader);

        // 2. Salviamo fisicamente il dato nel Database PostgreSQL
        torneoRepository.save(nuovoTorneo);

        return ResponseEntity.ok("Torneo salvato con successo nel database!");
    }

    /**
     * PUT: Aggiorna un torneo esistente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> aggiornaTorneo(
            @PathVariable Long id,
            @RequestBody Torneo torneoAggiornato,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Controlliamo se il torneo esiste prima di aggiornarlo
        Optional<Torneo> torneoEsistente = torneoRepository.findById(id);

        if (torneoEsistente.isPresent()) {
            // Se esiste, forziamo l'ID per essere sicuri di sovrascrivere quello giusto
            torneoAggiornato.setId(id); // Assicurati di avere il metodo setId() nella classe Torneo
            torneoRepository.save(torneoAggiornato);
            return ResponseEntity.ok("Torneo " + id + " aggiornato correttamente.");
        } else {
            return ResponseEntity.status(404).body("Errore: Torneo non trovato.");
        }
    }

    /**
     * DELETE: Elimina un torneo dal database.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminaTorneo(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (torneoRepository.existsById(id)) {
            torneoRepository.deleteById(id);
            return ResponseEntity.ok("Torneo " + id + " eliminato dal database.");
        } else {
            return ResponseEntity.status(404).body("Errore: Impossibile eliminare, torneo non trovato.");
        }
    }
}