package com.bitpub.controllers;

import com.bitpub.models.Torneo;
import com.bitpub.repository.TorneoRepository;
import com.bitpub.utils.HateoasResource; // Aggiunto per importare il Wrapper HATEOAS
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

    // 1. Colleghiamo il Controller al Database!
    @Autowired
    private TorneoRepository torneoRepository;

    /**
     * GET: Recupera un torneo e inietta i percorsi HATEOAS (Fase 13).
     */
    @GetMapping("/{id}")
    public ResponseEntity<HateoasResource<Torneo>> getTorneo(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

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

            // Restituiamo il JSON arricchito!
            return ResponseEntity.ok(risorsa);
        } else {
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

        System.out.println("[API REST] Richiesta creazione torneo. Token: " + authHeader);

        // Salviamo fisicamente il dato nel Database PostgreSQL
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