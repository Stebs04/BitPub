package com.bitpub.controllers;

import com.bitpub.assembler.UtenteModelAssembler;
import com.bitpub.dto.UtenteDTO;
import com.bitpub.services.UtenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    @Autowired
    private UtenteService utenteService;

    @Autowired
    private UtenteModelAssembler assembler;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CollectionModel<EntityModel<UtenteDTO>>> getAllUtenti(
            @RequestParam(required = false) String ruolo) {

        List<UtenteDTO> utenti = utenteService.getAllUtenti(ruolo);
        List<EntityModel<UtenteDTO>> utentiModel = utenti.stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        CollectionModel<EntityModel<UtenteDTO>> collectionModel = CollectionModel.of(utentiModel,
                linkTo(methodOn(UtenteController.class).getAllUtenti(ruolo)).withSelfRel()
        );

        return ResponseEntity.ok(collectionModel);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<UtenteDTO>> getUtenteById(@PathVariable Long id) {
        return utenteService.getUtenteById(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EntityModel<UtenteDTO>> creaUtente(@RequestBody UtenteDTO nuovoUtente) {
        UtenteDTO utenteSalvato = utenteService.creaUtente(nuovoUtente);
        return ResponseEntity.ok(assembler.toModel(utenteSalvato));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('UTENTE')")
    public ResponseEntity<EntityModel<UtenteDTO>> aggiornaUtente(@PathVariable Long id, @RequestBody UtenteDTO utenteAggiornato) {
        return utenteService.aggiornaUtente(id, utenteAggiornato)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminaUtente(@PathVariable Long id) {
        if(utenteService.eliminaUtente(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
