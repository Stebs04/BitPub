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
 * Controller REST dedicato alla gestione completa del ciclo di vita dei Tornei.
 * <p>
 * Questa classe espone gli endpoint per le operazioni CRUD (Create, Read, Update, Delete)
 * seguendo un'architettura State-Less. Utilizza il supporto HATEOAS per fornire link
 * dinamici nelle risposte, migliorando l'interazione con l'API.
 * </p>
 * * @author Timothy Giolito 20054431
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
     * Recupera i dettagli di un singolo torneo identificato dal suo ID univoco.
     * <p>
     * Se il torneo viene trovato, la risposta include i dati dell'oggetto e una serie
     * di link HATEOAS per facilitare le operazioni successive (aggiornamento, eliminazione, iscrizione).
     * </p>
     *
     * @param id L'identificativo univoco del torneo da cercare.
     * @return Una {@link ResponseEntity} contenente la risorsa {@link HateoasResource} se trovato,
     * oppure uno stato 404 (Not Found) se il torneo non esiste.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTorneoById(@PathVariable Long id) {

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
     * Crea un nuovo torneo e lo salva in modo persistente nel database.
     *
     * @param nuovoTorneo L'oggetto {@link Torneo} contenente i dati inviati nel corpo della richiesta (JSON).
     * @param authHeader (Opzionale) Header di autorizzazione per la sicurezza della richiesta.
     * @return Una {@link ResponseEntity} con un messaggio di conferma del salvataggio o un errore 500 in caso di problemi tecnici.
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
     * Aggiorna le informazioni di un torneo già esistente nel database.
     * <p>
     * Il metodo verifica l'esistenza del torneo tramite ID prima di procedere con la sovrascrittura dei dati.
     * </p>
     *
     * @param id L'ID del torneo da modificare.
     * @param torneoAggiornato L'oggetto {@link Torneo} con i nuovi dati da salvare.
     * @param authHeader (Opzionale) Header di autorizzazione.
     * @return {@link ResponseEntity} con esito positivo (200 OK) o errore (404 Not Found) se l'ID non esiste.
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
     * Rimuove un torneo dal database in modo definitivo.
     *
     * @param id L'identificativo del torneo da eliminare.
     * @param authHeader (Opzionale) Header di autorizzazione.
     * @return {@link ResponseEntity} con conferma dell'eliminazione o errore se il torneo non è stato trovato.
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
