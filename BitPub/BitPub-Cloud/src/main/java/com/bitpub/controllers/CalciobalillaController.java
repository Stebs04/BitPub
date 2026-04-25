package com.bitpub.controllers;

import com.bitpub.models.CalciobalillaStats;
import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.repository.PartitaCalciobalillaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// Import statici fondamentali per la generazione dinamica dei link
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST Controller per la gestione delle risorse relative alle partite di calciobalilla.
 * Aggiornato alla Fase 22 con supporto HATEOAS reale tramite Spring Hateoas e CollectionModel.
 *
 * @author Stefano Bellan 20054330 (Logica di Dominio)
 * @author Timothy (Integrazione HATEOAS Reale - Fase 22)
 */
@RestController
@RequestMapping(value = "/api/calciobalilla")
public class CalciobalillaController {

    @Autowired
    private PartitaCalciobalillaRepository repository;

    /**
     * Recupera l'elenco completo delle partite.
     * Restituisce un CollectionModel per permettere link a livello di collezione.
     */
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<PartitaCalciobalilla>>> getAllPartite() {
        List<EntityModel<PartitaCalciobalilla>> partite = repository.findAll().stream()
                .map(this::aggiungiLinkHateoas)
                .collect(Collectors.toList());

        // 1. Creiamo il CollectionModel avvolgendo la lista
        CollectionModel<EntityModel<PartitaCalciobalilla>> collectionModel = CollectionModel.of(partite);

        // 2. Aggiungiamo un link "self" alla collezione stessa
        collectionModel.add(linkTo(methodOn(CalciobalillaController.class).getAllPartite()).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Recupera una singola partita per ID con link ipertestuali dinamici.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PartitaCalciobalilla>> getPartitaById(@PathVariable Long id) {
        return repository.findById(id)
                .map(partita -> ResponseEntity.ok(aggiungiLinkHateoas(partita)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint per le statistiche aggregate.
     */
    @GetMapping("/stats")
    public ResponseEntity<CalciobalillaStats> getGlobalStats() {
        int rullate = repository.findAll().stream()
                .mapToInt(PartitaCalciobalilla::getTotaleRullate)
                .sum();
        int vRossi = repository.countVittorieRossi();
        int vBlu = repository.countVittorieBlu();

        return ResponseEntity.ok(new CalciobalillaStats(rullate, vRossi, vBlu));
    }

    /**
     * Arricchisce l'entità PartitaCalciobalilla con i metadati ipertestuali (Fase 22).
     */
    private EntityModel<PartitaCalciobalilla> aggiungiLinkHateoas(PartitaCalciobalilla partita) {
        EntityModel<PartitaCalciobalilla> risorsa = EntityModel.of(partita);

        // Link "self": punta alla risorsa corrente
        risorsa.add(linkTo(methodOn(CalciobalillaController.class).getPartitaById(partita.getId())).withSelfRel());

        // Link "storico": punta all'elenco completo
        risorsa.add(linkTo(methodOn(CalciobalillaController.class).getAllPartite()).withRel("storico"));

        // Link al Torneo: assicura il salto tra moduli in modo dinamico
        if (partita.getTorneoId() != null) {
            risorsa.add(linkTo(methodOn(TorneoController.class).getTorneoById(partita.getTorneoId())).withRel("dettagli_torneo"));
        }

        return risorsa;
    }
}
