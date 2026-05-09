package com.bitpub.controllers;

import com.bitpub.dto.PartitaCalciobalillaDTO;
import com.bitpub.models.CalciobalillaStats;
import com.bitpub.services.CalciobalillaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/calciobalilla")
@CrossOrigin(origins = "*")
public class CalciobalillaController {

    @Autowired
    private CalciobalillaService service;

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<PartitaCalciobalillaDTO>>> getAllPartite() {
        List<EntityModel<PartitaCalciobalillaDTO>> partite = service.getAllPartite().stream()
                .map(this::aggiungiLinkHateoas)
                .collect(Collectors.toList());

        CollectionModel<EntityModel<PartitaCalciobalillaDTO>> collectionModel = CollectionModel.of(partite);
        collectionModel.add(linkTo(methodOn(CalciobalillaController.class).getAllPartite()).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PartitaCalciobalillaDTO>> getPartitaById(@PathVariable("id") Long id) {
        return service.getPartitaById(id)
                .map(partita -> ResponseEntity.ok(aggiungiLinkHateoas(partita)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<CalciobalillaStats> getGlobalStats() {
        return ResponseEntity.ok(service.getGlobalStats());
    }

    private EntityModel<PartitaCalciobalillaDTO> aggiungiLinkHateoas(PartitaCalciobalillaDTO partita) {
        EntityModel<PartitaCalciobalillaDTO> risorsa = EntityModel.of(partita);

        risorsa.add(linkTo(methodOn(CalciobalillaController.class).getPartitaById(partita.getId())).withSelfRel());
        risorsa.add(linkTo(methodOn(CalciobalillaController.class).getAllPartite()).withRel("storico"));

        if (partita.getTorneoId() != null) {
            risorsa.add(linkTo(methodOn(TorneoController.class).getTorneoById(partita.getTorneoId()))
                    .withRel("dettagli_torneo"));
        }

        return risorsa;
    }
}
