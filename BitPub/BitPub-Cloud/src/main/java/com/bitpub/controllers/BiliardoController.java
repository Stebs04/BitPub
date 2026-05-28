package com.bitpub.controllers;

import com.bitpub.assembler.GameEventModelAssembler;
import com.bitpub.dto.GameEventDTO;
import com.bitpub.services.ElaborazioneEventiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * BiliardoController - Gestione eventi real-time del gioco Biliardo.
 *
 * Refactoring Senior Note:
 * Rimosso l'uso della vecchia BiliardoResource.
 * Adottato lo standard GameEventDTO + GameEventModelAssembler per una 
 * corretta separazione tra layer di business e layer di presentazione REST.
 * @author Luca Franzon
 */
@RestController
@RequestMapping("/api/v1/biliardo")
public class BiliardoController {

    private final ElaborazioneEventiService eventiService;
    private final GameEventModelAssembler assembler;

    @Autowired
    public BiliardoController(ElaborazioneEventiService eventiService, GameEventModelAssembler assembler) {
        this.eventiService = eventiService;
        this.assembler = assembler;
    }

    @GetMapping("/event/{id}")
    public ResponseEntity<EntityModel<GameEventDTO>> getEventById(@PathVariable Long id) {
        // Recupero dal service e trasformazione tramite assembler
        GameEventDTO dto = eventiService.getEventDtoById(id);
        return ResponseEntity.ok(assembler.toModel(dto));
    }

    @GetMapping("/session/{sessionId}/events")
    public ResponseEntity<CollectionModel<EntityModel<GameEventDTO>>> getEventsBySession(@PathVariable Long sessionId) {
        List<GameEventDTO> eventi = eventiService.getEventsBySession(sessionId);

        List<EntityModel<GameEventDTO>> resources = eventi.stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                CollectionModel.of(resources, 
                        linkTo(methodOn(BiliardoController.class).getEventsBySession(sessionId)).withSelfRel())
        );
    }
}
