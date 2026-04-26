package com.bitpub.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bitpub.models.Torneo;
import com.bitpub.repository.TorneoRepository;
import com.bitpub.assembler.TorneoModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.bitpub.models.Utente;
import com.bitpub.repository.UtenteRepository;

/**
 * Controller REST dedicato alla gestione completa del ciclo di vita dei Tornei.
 * <p>
 * Questa classe espone gli endpoint per le operazioni CRUD (Create, Read, Update, Delete)
 * seguendo un'architettura State-Less. Utilizza il supporto HATEOAS per fornire link
 * dinamici nelle risposte, migliorando l'interazione con l'API.
 * </p>
 * * @author Timothy Giolito 20054431
 * @author Stefano Bellan 20054330 (Assembler per esposizione link HATEOAS)
 */
@RestController
@RequestMapping("/api/tornei")
public class TorneoController {

    private static final Logger log = LoggerFactory.getLogger(TorneoController.class);

    @Autowired
    private TorneoRepository torneoRepository;

    @Autowired
    private TorneoModelAssembler assembler;

    @Autowired
    private UtenteRepository utenteRepository;

    /**
     * Recupera i dettagli di un singolo torneo identificato dal suo ID univoco.
     * <p>
     * Se il torneo viene trovato, la risposta include i dati dell'oggetto e una serie
     * di link HATEOAS per facilitare le operazioni successive.
     * </p>
     *
     * @param id L'identificativo univoco del torneo da cercare.
     * @return Una ResponseEntity contenente la risorsa HATEOAS se trovato, oppure stato 404 (Not Found).
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Torneo>> getTorneoById(@PathVariable("id") Long id) {

        log.info("Ricevuta richiesta GET su /api/tornei/{} - Ricerca torneo in corso", id);
        Optional<Torneo> torneoTrovato = torneoRepository.findById(id);

        if (torneoTrovato.isPresent()) {
            Torneo torneo = torneoTrovato.get();
            EntityModel<Torneo> resource = assembler.toModel(torneo);
            
            log.info("Torneo {} trovato con successo. Restituzione risorsa HATEOAS.", id);
            return ResponseEntity.ok(resource);
        } else {
            log.warn("Attenzione: Torneo con ID {} non trovato nel database.", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint per il recupero della lista completa dei tornei attivi e passati.
     *
     * @return ResponseEntity contenente la collezione di tornei arricchita con i link ipertestuali (HATEOAS).
     */
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<Torneo>>> getAllTornei() {
        List<Torneo> tornei = torneoRepository.findAll();

        List<EntityModel<Torneo>> torneiModel = tornei.stream()
                .map(assembler::toModel)
                .toList();

        CollectionModel<EntityModel<Torneo>> collectionModel = CollectionModel.of(torneiModel,
                linkTo(methodOn(TorneoController.class).getAllTornei()).withSelfRel()
        );

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Crea un nuovo torneo e lo salva in modo persistente nel database.
     *
     * @param nuovoTorneo L'oggetto Torneo deserializzato dal JSON della richiesta.
     * @param authHeader  (Opzionale) Header di autorizzazione JWT.
     * @return ResponseEntity con messaggio di conferma (200 OK) o errore (500 Server Error).
     */
    @PostMapping
    public ResponseEntity<String> creaTorneo(
            @RequestBody Torneo nuovoTorneo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Ricevuta richiesta POST su /api/tornei - Creazione nuovo torneo");

        try {
            torneoRepository.save(nuovoTorneo);
            log.info("Torneo salvato con successo nel database PostgreSQL!");
            return ResponseEntity.ok("Torneo salvato con successo nel database!");

        } catch (Exception e) {
            log.error("ERRORE CRITICO durante il salvataggio del torneo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Errore interno durante il salvataggio.");
        }
    }

    /**
     * Sovrascrive/aggiorna integralmente i dettagli di un torneo esistente.
     * * @param id               L'identificativo del torneo da aggiornare.
     * @param torneoAggiornato L'entità torneo con i nuovi dati da applicare.
     * @param authHeader       Header di sicurezza per il controllo accessi.
     * @return ResponseEntity confermante l'avvenuta modifica o 404 se inesistente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> aggiornaTorneo(
            @PathVariable("id") Long id,
            @RequestBody Torneo torneoAggiornato,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Ricevuta richiesta PUT su /api/tornei/{} - Aggiornamento torneo in corso", id);
        Optional<Torneo> torneoEsistente = torneoRepository.findById(id);

        if (torneoEsistente.isPresent()) {
            // Impedisce ID hijacking garantendo che l'entità mantenga l'ID della risorsa URL
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
     * Rimuove in modo permanente un torneo dal sistema.
     * * @param id         L'identificativo del torneo da eliminare.
     * @param authHeader Header per verifica permessi autorizzativi.
     * @return ResponseEntity di avvenuta eliminazione.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminaTorneo(
            @PathVariable("id") Long id,
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

    /**
     * Endpoint HATEOAS per recuperare dinamicamente le info sulla prossima partita di un torneo in corso.
     *
     * @param id Identificativo univoco del torneo.
     * @return ResponseEntity contenente i dettagli testuali della prossima partita.
     */
    @GetMapping("/{id}/prossima-partita")
    public ResponseEntity<String> getProssimaPartita(@PathVariable("id") Long id) {
        return ResponseEntity.ok("Il server ha fornito questo url HATEOAS. Prossima partita per il torneo: " + id);
    }

    /**
     * Gestisce la logica business di iscrizione di un utente a uno specifico torneo.
     * Valida i limiti di capacità massimi definiti in fase di creazione.
     *
     * @param id       L'identificativo del torneo.
     * @param utenteId L'ID dell'utente che avanza la richiesta di iscrizione.
     * @return ResponseEntity indicante esito positivo o fallimento (Limiti superati, già iscritto).
     */
    @PostMapping("/{id}/iscrivi")
    public ResponseEntity<String> iscriviUtente(
            @PathVariable("id") Long id,
            @RequestParam("utenteId") Long utenteId) {

        log.info("Richiesta di iscrizione utente {} al torneo {}", utenteId, id);

        Optional<Torneo> torneoOpt = torneoRepository.findById(id);
        Optional<Utente> utenteOpt = utenteRepository.findById(utenteId);

        if (torneoOpt.isEmpty() || utenteOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Torneo o Utente non trovato.");
        }

        Torneo torneo = torneoOpt.get();
        Utente utente = utenteOpt.get();

        // Controllo capienza torneo per evitare Overbooking
        if (torneo.getMaxPartecipanti() != null) {
            int iscrittiAttuali = (torneo.getIscritti() != null) ? torneo.getIscritti().size() : 0;
            if (iscrittiAttuali >= torneo.getMaxPartecipanti()) {
                log.warn("Il torneo {} ha raggiunto il limite massimo di iscritti.", id);
                return ResponseEntity.status(400).body("Errore: Iscrizioni chiuse. Raggiunto il limite massimo di partecipanti.");
            }
        }

        if (torneo.getIscritti() == null) {
            torneo.setIscritti(new java.util.ArrayList<>());
        }
        
        if (!torneo.getIscritti().contains(utente)) {
            torneo.getIscritti().add(utente);
            torneoRepository.save(torneo);
            log.info("Utente {} iscritto con successo al torneo {}", utenteId, id);
            return ResponseEntity.ok("Iscrizione completata con successo!");
        } else {
            return ResponseEntity.status(400).body("L'utente è già iscritto al torneo.");
        }
    }
}