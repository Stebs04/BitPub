package com.bitpub.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bitpub.dto.TorneoDTO;
import com.bitpub.services.TorneoService;
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

@RestController
@RequestMapping("/api/tornei")
public class TorneoController {

    private static final Logger log = LoggerFactory.getLogger(TorneoController.class);

    @Autowired
    private TorneoService torneoService;

    @Autowired
    private TorneoModelAssembler assembler;

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<TorneoDTO>> getTorneoById(@PathVariable("id") Long id) {
        log.info("Ricevuta richiesta GET su /api/tornei/{} - Ricerca torneo in corso", id);
        Optional<TorneoDTO> torneoTrovato = torneoService.getTorneoById(id);

        if (torneoTrovato.isPresent()) {
            TorneoDTO torneo = torneoTrovato.get();
            EntityModel<TorneoDTO> resource = assembler.toModel(torneo);
            log.info("Torneo {} trovato con successo. Restituzione risorsa HATEOAS.", id);
            return ResponseEntity.ok(resource);
        } else {
            log.warn("Attenzione: Torneo con ID {} non trovato.", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<TorneoDTO>>> getAllTornei() {
        List<TorneoDTO> tornei = torneoService.getAllTornei();
        List<EntityModel<TorneoDTO>> torneiModel = tornei.stream()
                .map(assembler::toModel)
                .toList();

        CollectionModel<EntityModel<TorneoDTO>> collectionModel = CollectionModel.of(torneiModel,
                linkTo(methodOn(TorneoController.class).getAllTornei()).withSelfRel()
        );
        return ResponseEntity.ok(collectionModel);
    }

    @PostMapping
    public ResponseEntity<?> creaTorneo(
            @RequestBody TorneoDTO nuovoTorneo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            TorneoDTO torneoSalvato = torneoService.creaTorneo(nuovoTorneo);
            return ResponseEntity.ok(assembler.toModel(torneoSalvato));
        } catch (Exception e) {
            log.error("Errore durante la creazione del torneo", e);
            return ResponseEntity.internalServerError().body(java.util.Map.of("errore", "Errore interno"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> aggiornaTorneo(@PathVariable("id") Long id, @RequestBody TorneoDTO datiAggiornati) {
        return torneoService.aggiornaTorneo(id, datiAggiornati)
                .map(salvato -> ResponseEntity.ok(assembler.toModel(salvato)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminaTorneo(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (torneoService.eliminaTorneo(id)) {
            return ResponseEntity.ok("Torneo " + id + " eliminato.");
        } else {
            return ResponseEntity.status(404).body("Errore: Impossibile eliminare, torneo non trovato.");
        }
    }

    @GetMapping("/{id}/prossima-partita")
    public ResponseEntity<String> getProssimaPartita(@PathVariable("id") Long id) {
        return ResponseEntity.ok("Il server ha fornito questo url HATEOAS. Prossima partita per il torneo: " + id);
    }
    
    @GetMapping("/{id}/prossimo-match")
    public ResponseEntity<String> getProssimoMatch(@PathVariable("id") Long id) {
        return getProssimaPartita(id);
    }

    @PostMapping("/{id}/iscrivi")
    public ResponseEntity<String> iscriviUtente(
            @PathVariable("id") Long id,
            @RequestParam("utenteId") Long utenteId) {

        if (torneoService.iscriviUtente(id, utenteId)) {
            return ResponseEntity.ok("Utente " + utenteId + " iscritto con successo al torneo " + id);
        } else {
            return ResponseEntity.badRequest().body("Impossibile iscrivere l'utente: torneo o utente inesistente.");
        }
    }
}
