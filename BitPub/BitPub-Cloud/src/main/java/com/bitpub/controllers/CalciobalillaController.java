package com.bitpub.controllers;

import com.bitpub.assembler.GameEventModelAssembler;
import com.bitpub.dto.GameEventDTO;
import com.bitpub.services.CalciobalillaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * CalciobalillaController - Gestione eventi real-time del gioco Calciobalilla.
 *
 * Refactoring Senior Note:
 * Il controller è stato allineato al nuovo standard architetturale del progetto.
 * Restituisce esclusivamente GameEventDTO validati tramite GameEventModelAssembler,
 * garantendo coerenza totale con gli altri moduli di gioco (es. Biliardo) e
 * disaccoppiando l'API REST dalle Entity del database.
 */
@RestController
@RequestMapping("/api/v1/calciobalilla")
public class CalciobalillaController {

    private final CalciobalillaService calciobalillaService;
    private final GameEventModelAssembler assembler;

    @Autowired
    public CalciobalillaController(CalciobalillaService calciobalillaService, GameEventModelAssembler assembler) {
        this.calciobalillaService = calciobalillaService;
        this.assembler = assembler;
    }

    /**
     * Recupera il dettaglio di un singolo evento di gioco tramite il suo identificativo.
     */
    @GetMapping("/event/{id}")
    public ResponseEntity<EntityModel<GameEventDTO>> getEventById(@PathVariable Long id) {
        // Il service si occupa del recupero e della conversione iniziale in DTO
        GameEventDTO dto = calciobalillaService.getEventDtoById(id);
        
        // L'assembler inietta i link HATEOAS per la navigabilità
        return ResponseEntity.ok(assembler.toModel(dto));
    }

    /**
     * Recupera l'intero storico degli eventi associati a una specifica sessione di gioco.
     */
    @GetMapping("/session/{sessionId}/events")
    public ResponseEntity<CollectionModel<EntityModel<GameEventDTO>>> getEventsBySession(@PathVariable Long sessionId) {
        // Delega la lettura massiva al layer di servizio
        List<GameEventDTO> eventi = calciobalillaService.getEventsBySession(sessionId);

        // Mappatura della collezione tramite lo standard assembler
        List<EntityModel<GameEventDTO>> resources = eventi.stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        // Costruzione del contenitore HATEOAS con il link self
        return ResponseEntity.ok(
                CollectionModel.of(resources, 
                        linkTo(methodOn(CalciobalillaController.class).getEventsBySession(sessionId)).withSelfRel())
        );
    }
}