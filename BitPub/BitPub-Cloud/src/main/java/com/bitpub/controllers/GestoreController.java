package com.bitpub.controllers;

import com.bitpub.assembler.LocaleModelAssembler;
import com.bitpub.dto.LocaleDTO;
import com.bitpub.models.Locale;
import com.bitpub.services.LocaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.bitpub.services.StatisticheService;
import com.bitpub.dto.StatisticheLocaleDTO;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * GestoreController - Entrypoint dedicato alle operazioni per il profilo 'Gestore'.
 * * Refactoring Senior Note:
 * È stato rimosso l'anti-pattern dell'accesso diretto alla Repository.
 * Adesso il controller delega interamente al Service Layer (LocaleService),
 * garantendo che la business logic sia centralizzata e non dispersa negli endpoint.
 * Viene garantito l'incapsulamento delle Entity JPA restituendo esclusivamente DTO
 * mappati tramite ModelAssembler per supportare lo standard HATEOAS.
 */
@RestController
@RequestMapping("/api/v1/gestore")
@PreAuthorize("hasRole('GESTORE')")
@CrossOrigin(origins = "*")
public class GestoreController {

    private final LocaleService localeService;
    private final LocaleModelAssembler localeAssembler;
    private final StatisticheService statisticheService;

    @Autowired
    public GestoreController(LocaleService localeService, LocaleModelAssembler localeAssembler, StatisticheService statisticheService) {
        this.localeService = localeService;
        this.localeAssembler = localeAssembler;
        this.statisticheService = statisticheService;
    }

    /**
     * Recupera le statistiche avanzate per il locale richiesto
     */
    @GetMapping("locale/{id}/statistiche")
    public ResponseEntity<StatisticheLocaleDTO> getStatisticheLocale(@PathVariable Long id) {
        StatisticheLocaleDTO stats = statisticheService.calcolaStatisticheCalciobalilla(id);
        return ResponseEntity.ok(stats);
    }

    /**
     * Recupera la lista di tutti i locali associati a un determinato gestore.
     * * @param idGestore Identificativo univoco del gestore.
     * @return CollectionModel contenente i DTO dei locali e i relativi link HATEOAS.
     */
    @GetMapping("/locali/{idGestore}")
    public ResponseEntity<CollectionModel<EntityModel<LocaleDTO>>> getLocaliPerGestore(@PathVariable Long idGestore) {
        // Delega al service la logica di filtraggio e recupero
        List<LocaleDTO> locali = localeService.getLocaliByGestoreId(idGestore);

        // Trasformazione in DTO tramite l'assembler dedicato per isolare il layer di persistenza
        List<EntityModel<LocaleDTO>> localeResources = locali.stream()
                .map(localeAssembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                CollectionModel.of(localeResources,
                        linkTo(methodOn(GestoreController.class).getLocaliPerGestore(idGestore)).withSelfRel())
        );
    }

    /**
     * Recupera il dettaglio di un singolo locale gestito, previa verifica di appartenenza.
     * * @param id Identificativo del locale.
     * @return EntityModel del LocaleDTO.
     */
    @GetMapping("/locale/{id}")
    public ResponseEntity<EntityModel<LocaleDTO>> getLocaleDettaglio(@PathVariable Long id) {
        // Il service si occupa di gestire l'eventuale EntityNotFoundException
        LocaleDTO locale = localeService.getLocaleById(id).orElseThrow(() -> new RuntimeException("Locale non trovato"));
        
        return ResponseEntity.ok(localeAssembler.toModel(locale));
    }
}
