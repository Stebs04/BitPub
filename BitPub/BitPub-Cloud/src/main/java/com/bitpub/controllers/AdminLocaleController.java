package com.bitpub.controllers;

import com.bitpub.assembler.LocaleModelAssembler;
import com.bitpub.dto.LocaleDTO;
import com.bitpub.services.LocaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locali")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminLocaleController {

    @Autowired
    private LocaleService localeService;

    @Autowired
    private LocaleModelAssembler assembler;

    @GetMapping
    public ResponseEntity<List<LocaleDTO>> getAll() {
        return ResponseEntity.ok(localeService.getAllLocali());
    }

    @PostMapping
    public ResponseEntity<EntityModel<LocaleDTO>> crea(@RequestBody LocaleDTO locale) {
        LocaleDTO salvato = localeService.creaLocale(locale);
        return ResponseEntity.ok(assembler.toModel(salvato));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<LocaleDTO>> aggiorna(@PathVariable Long id, @RequestBody LocaleDTO datiAggiornati) {
        return localeService.aggiornaLocale(id, datiAggiornati)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> elimina(@PathVariable Long id) {
        if(localeService.eliminaLocale(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
