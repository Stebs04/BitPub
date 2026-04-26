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

/**
 * Controller REST dedicato alla gestione completa del ciclo di vita dei Tornei.
 * <p>
 * Questa classe espone gli endpoint per le operazioni CRUD (Create, Read, Update, Delete)
 * seguendo un'architettura State-Less. Utilizza il supporto HATEOAS per fornire link
 * dinamici nelle risposte, migliorando l'interazione con l'API.
 * </p>
 * * @author Timothy Giolito 20054431
 * @athor Stefano Bellan 20054330 (Assembler per esposizione link HATEOAS)
 */
@RestController
@RequestMapping("/api/tornei")
public class TorneoController {

    // Inizializziamo il Logger specifico per questa classe!
    private static final Logger log = LoggerFactory.getLogger(TorneoController.class);

    // Colleghiamo il Controller al Database!
    @Autowired
    private TorneoRepository torneoRepository;

    @Autowired
    private TorneoModelAssembler assembler;

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
     * @author Timothy Giolito 20054431 e Modificato da: Stefano Bellan 20054330
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Torneo>> getTorneoById(@PathVariable("id") Long id) {

        log.info("Ricevuta richiesta GET su /api/tornei/{} - Ricerca torneo in corso", id);

        // Cerchiamo il torneo nel DB
        Optional<Torneo> torneoTrovato = torneoRepository.findById(id);

        if (torneoTrovato.isPresent()) {
            Torneo torneo = torneoTrovato.get();
            
            //Modifica fatta da Stefano: Delego la creazione dei link dinamici all'Assembler
            EntityModel<Torneo> resource = assembler.toModel(torneo);
            
            log.info("Torneo {} trovato con successo. Restituzione risorsa HATEOAS.", id);
            return ResponseEntity.ok(resource);
        } else {
            log.warn("Attenzione: Torneo con ID {} non trovato nel database.", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint per il recupero della lista completa dei tornei.
     * <p>
     * Questo metodo interroga il repository per ottenere tutte le istanze di {@link Torneo},
     * le trasforma in {@link EntityModel} tramite l'assembler dedicato e le incapsula 
     * in un {@link CollectionModel} per rispettare lo standard HATEOAS.
     * @author Stefano Bellan 20054330
     * </p>
     *
     * @return {@link ResponseEntity} contenente la collezione di tornei arricchita con i link ipertestuali.
     */
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<Torneo>>> getAllTornei() {
        // Recupero di tutte le entità di dominio dal database
        List<Torneo> tornei = torneoRepository.findAll();

        // Trasformazione della lista di entità in una lista di modelli HATEOAS
        // L'assembler applica automaticamente la logica condizionale (es. link "nextMatch")
        List<EntityModel<Torneo>> torneiModel = tornei.stream()
                .map(assembler::toModel)
                .toList();

        // Creazione del wrapper per la collezione con link self al punto di ingresso dell'API
        CollectionModel<EntityModel<Torneo>> collectionModel = CollectionModel.of(torneiModel,
                linkTo(methodOn(TorneoController.class).getAllTornei()).withSelfRel()
        );

        return ResponseEntity.ok(collectionModel);
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
     * PUT: Aggiorna un torneo esistente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> aggiornaTorneo(
            @PathVariable("id") Long id,
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
     * Recupera le informazioni sulla prossima partita programmata per un torneo specifico.
     * <p>
     * Questo endpoint viene esposto dinamicamente dall'assembler solo se il torneo 
     * soddisfa i requisiti temporali (non concluso). Fornisce un punto di accesso 
     * diretto senza che il client debba filtrare manualmente la lista dei match.
     * </p>
     *
     * @param id Identificativo univoco del torneo.
     * @return {@link ResponseEntity} contenente i dettagli della prossima partita (attualmente in formato testuale).
     * @author Stefano Bellan 20054330
     */
    @GetMapping("/{id}/prossima-partita")
    public ResponseEntity<String> getProssimaPartita(@PathVariable("id") Long id) {
        return ResponseEntity.ok("Il server ha fornito questo url HATEOAS. Prossima partita per il torneo: " + id);
    }

}
