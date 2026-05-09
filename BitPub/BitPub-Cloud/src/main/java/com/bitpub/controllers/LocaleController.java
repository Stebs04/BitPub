package com.bitpub.controllers;

import com.bitpub.assembler.LocaleModelAssembler;
import com.bitpub.dto.LocaleDTO;
import com.bitpub.services.LocaleService;
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
@RequestMapping("/api/locali")
public class LocaleController {

    @Autowired
    private LocaleService localeService;

    @Autowired
    private LocaleModelAssembler assembler;

    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<LocaleDTO>>> getAllLocali() {
        List<LocaleDTO> locali = localeService.getAllLocali();

        List<EntityModel<LocaleDTO>> localiModel = locali.stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        CollectionModel<EntityModel<LocaleDTO>> collectionModel = CollectionModel.of(localiModel,
                linkTo(methodOn(LocaleController.class).getAllLocali()).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<LocaleDTO>> getLocaleById(@PathVariable("id") Long id) {
        return localeService.getLocaleById(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EntityModel<LocaleDTO>> creaLocale(@RequestBody LocaleDTO nuovoLocale) {
        LocaleDTO localeSalvato = localeService.creaLocale(nuovoLocale);
        return ResponseEntity.ok(assembler.toModel(localeSalvato));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<LocaleDTO>> aggiornaLocale(@PathVariable("id") Long id, @RequestBody LocaleDTO localeAggiornato) {
        return localeService.aggiornaLocale(id, localeAggiornato)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminaLocale(@PathVariable("id") Long id) {
        if(localeService.eliminaLocale(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
