package com.bitpub.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bitpub.models.Torneo;
import com.bitpub.repository.TorneoRepository;
import com.bitpub.utils.HateoasResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller REST per la gestione dei Tornei.
 * Architettura rigorosamente State-Less con supporto HATEOAS.
 */
@RestController
@RequestMapping("/api/tornei")
public class TorneoController {

    // Inizializziamo il Logger specifico per questa classe!
    private static final Logger log = LoggerFactory.getLogger(TorneoController.class);

    // Colleghiamo il Controller al Database!
    @Autowired
    private TorneoRepository torneoRepository;

    /**
     * GET: Recupera un torneo e inietta i percorsi HATEOAS (Fase 13).
     */
    @GetMapping("/{id}")
    public ResponseEntity<HateoasResource<Torneo>> getTorneo(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Ricevuta richiesta GET su /api/tornei/{} - Ricerca torneo in corso", id);

        // Cerchiamo il torneo nel DB
        Optional<Torneo> torneoTrovato = torneoRepository.findById(id);

        if (torneoTrovato.isPresent()) {
            Torneo torneo = torneoTrovato.get();

            // Creiamo il contenitore HATEOAS con i dati del torneo
            HateoasResource<Torneo> risorsa = new HateoasResource<>(torneo);

            // TIMOTHY: Iniettiamo i percorsi dinamici (Il nodo _links)
            risorsa.addLink("self", "/api/tornei/" + id);
            risorsa.addLink("aggiorna_torneo", "/api/tornei/" + id);
            risorsa.addLink("elimina_torneo", "/api/tornei/" + id);
            risorsa.addLink("iscrivi_partecipanti", "/api/tornei/" + id + "/partecipanti");

            log.info("Torneo {} trovato con successo. Restituzione risorsa HATEOAS.", id);
            return ResponseEntity.ok(risorsa);
        } else {
            log.warn("Attenzione: Torneo con ID {} non trovato nel database.", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST: Crea e salva un nuovo torneo nel database.
     */
    @PostMapping
    public ResponseEntity<String> creaTorneo(
            @RequestBody Torneo nuovoTorneo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Ricevuta richiesta POST su /api/tornei - Creazione nuovo torneo");

        try {
            // Salviamo fisicamente il dato nel Database PostgreSQL
            torneoRepository.save(nuovoTorneo);
            log.info("Torneo salvato con successo nel database PostgreSQL!");
            return ResponseEntity.ok("Torneo salvato con successo nel database!");

        } catch (Exception e) {
            // Logghiamo l'errore se il database rifiuta il salvataggio
            log.error("ERRORE CRITICO durante il salvataggio del torneo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Errore interno durante il salvataggio.");
        }
    }

    /**
     * PUT: Aggiorna un torneo esistente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> aggiornaTorneo(
            @PathVariable Long id,
            @RequestBody Torneo torneoAggiornato,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Ricevuta richiesta PUT su /api/tornei/{} - Aggiornamento torneo in corso", id);

        // Controlliamo se il torneo esiste prima di aggiornarlo
        Optional<Torneo> torneoEsistente = torneoRepository.findById(id);

        if (torneoEsistente.isPresent()) {
            // Se esiste, forziamo l'ID per essere sicuri di sovrascrivere quello giusto
            torneoAggiornato.setId(id);
            torneoRepository.save(torneoAggiornato);

            log.info("Torneo {} aggiornato correttamente nel database.", id);
            return ResponseEntity.ok("Torneo " + id + " aggiornato correttamente.");
        } else {
            log.warn("Impossibile aggiornare: Torneo con ID {} non trovato.", id);
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

        log.info("Ricevuta richiesta DELETE su /api/tornei/{} - Eliminazione torneo in corso", id);

        if (torneoRepository.existsById(id)) {
            torneoRepository.deleteById(id);
            log.info("Torneo {} eliminato con successo dal database.", id);
            return ResponseEntity.ok("Torneo " + id + " eliminato dal database.");
        } else {
            log.warn("Impossibile eliminare: Torneo con ID {} non trovato.", id);
            return ResponseEntity.status(404).body("Errore: Impossibile eliminare, torneo non trovato.");
        }
    }
}