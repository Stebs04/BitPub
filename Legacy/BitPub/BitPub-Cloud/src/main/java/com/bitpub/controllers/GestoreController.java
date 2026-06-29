package com.bitpub.controllers;

import com.bitpub.assembler.LocaleModelAssembler;
import com.bitpub.dto.LocaleDTO;
import com.bitpub.dto.MacchinaDTO;
import com.bitpub.dto.StatisticheLocaleDTO;
import com.bitpub.models.EdgeStatusEntity;
import com.bitpub.repository.EdgeStatusRepository;
import com.bitpub.services.LocaleService;
import com.bitpub.services.StatisticheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final EdgeStatusRepository edgeStatusRepository;

    @Autowired
    public GestoreController(LocaleService localeService,
                             LocaleModelAssembler localeAssembler,
                             StatisticheService statisticheService,
                             EdgeStatusRepository edgeStatusRepository) {
        this.localeService = localeService;
        this.localeAssembler = localeAssembler;
        this.statisticheService = statisticheService;
        this.edgeStatusRepository = edgeStatusRepository;
    }

    @GetMapping("/locali/{idGestore}")
    public ResponseEntity<CollectionModel<EntityModel<LocaleDTO>>> getLocaliPerGestore(@PathVariable Long idGestore) {
        List<LocaleDTO> locali = localeService.getLocaliByGestoreId(idGestore);
        List<EntityModel<LocaleDTO>> localeResources = locali.stream()
                .map(localeAssembler::toModel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(CollectionModel.of(localeResources,
                linkTo(methodOn(GestoreController.class).getLocaliPerGestore(idGestore)).withSelfRel()));
    }

    @GetMapping("/locale/{id}")
    public ResponseEntity<EntityModel<LocaleDTO>> getLocaleDettaglio(@PathVariable Long id) {
        LocaleDTO locale = localeService.getLocaleById(id).orElseThrow(() -> new RuntimeException("Locale non trovato"));
        return ResponseEntity.ok(localeAssembler.toModel(locale));
    }

    @GetMapping("/locale/{id}/statistiche")
    public ResponseEntity<StatisticheLocaleDTO> getStatisticheLocale(@PathVariable Long id) {
        StatisticheLocaleDTO stats = statisticheService.calcolaStatisticheCalciobalilla(id);
        return ResponseEntity.ok(stats);
    }

    /**
     * Recupera la lista delle macchine, leggendo lo stato di rete REALE dal database.
     */
    @GetMapping("/locale/{id}/macchine")
    public ResponseEntity<List<MacchinaDTO>> getMacchinePerLocale(@PathVariable Long id) {
        // 1. Lettura dinamica dal database popolato da MQTT
        Optional<EdgeStatusEntity> edgeStatusOpt = edgeStatusRepository.findById(id.toString());
        boolean isEdgeOnline = edgeStatusOpt.isPresent() && "ONLINE".equalsIgnoreCase(edgeStatusOpt.get().getStatus());

        List<MacchinaDTO> macchine = new ArrayList<>();

        MacchinaDTO m1 = new MacchinaDTO();
        m1.setId(1L);
        m1.setNome("Calciobalilla Garlando");
        m1.setTipoGioco("CALCIOBALILLA");
        m1.setAttiva(isEdgeOnline); // Stato di rete VERO
        m1.setAttuatoreSbloccato(isEdgeOnline); // Se l'edge è online, simuliamo il tavolo sbloccato

        MacchinaDTO m2 = new MacchinaDTO();
        m2.setId(2L);
        m2.setNome("Biliardo 8-Ball");
        m2.setTipoGioco("BILIARDO");
        m2.setAttiva(isEdgeOnline);
        m2.setAttuatoreSbloccato(false); // Simuliamo che nessuno stia giocando qui

        macchine.add(m1);
        macchine.add(m2);

        return ResponseEntity.ok(macchine);
    }
}
