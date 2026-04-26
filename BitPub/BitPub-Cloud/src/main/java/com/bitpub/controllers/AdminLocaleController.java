package com.bitpub.controllers;

import com.bitpub.models.Locale;
import com.bitpub.repository.LocaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller per la Dashboard ADMIN.
 * Gestisce i locali visibili solo agli amministratori.
 *
 * @author BitPub Team
 * @version 1.0
 */
@RestController
@RequestMapping(value = "/api/v1/admin/locali", produces = "application/resources.v1+json")
public class AdminLocaleController {

    @Autowired
    private LocaleRepository localeRepository;

    /**
     * Recupera l'elenco di tutti i locali per la dashboard Admin.
     *
     * @return {@link CollectionModel} contenente i locali arricchiti con link HATEOAS.
     */
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<Locale>>> getAll() {
        List<EntityModel<Locale>> localiModel = localeRepository.findAll().stream()
                .map(this::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                CollectionModel.of(localiModel,
                        linkTo(methodOn(AdminLocaleController.class).getAll()).withSelfRel()
                )
        );
    }

    /**
     * Crea un nuovo locale.
     *
     * @param nuovo Dati del nuovo locale da creare (nome, indirizzo, citta, capienza, gestoreId).
     * @return 201 Created con i dati salvati e links.
     */
    @PostMapping
    public ResponseEntity<?> crea(@RequestBody Locale nuovo) {
        // Validazioni base
        if (nuovo.getName() == null || nuovo.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Nome mancante");
        }

        if (localeRepository.existsByName(nuovo.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Nome locale già esistente.");
        }

        // Dummy value per l'ip se usato solo da edge in logica estesa
        if (nuovo.getIpAddressEdge() == null) {
            nuovo.setIpAddressEdge("Da configurare");
        }

        Locale salvato = localeRepository.save(nuovo);
        return ResponseEntity.status(HttpStatus.CREATED).body(toModel(salvato));
    }

    /**
     * Modifica un locale esistente.
     *
     * @param id ID del locale da modificare.
     * @param datiAggiornati Nuovi dati.
     * @return Modello aggiornato o 404.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> aggiorna(@PathVariable("id") Long id, @RequestBody Locale datiAggiornati) {
        return localeRepository.findById(id).map(esistente -> {
            if (datiAggiornati.getName() != null) esistente.setName(datiAggiornati.getName());
            if (datiAggiornati.getIndirizzo() != null) esistente.setIndirizzo(datiAggiornati.getIndirizzo());
            if (datiAggiornati.getCitta() != null) esistente.setCitta(datiAggiornati.getCitta());
            if (datiAggiornati.getCapienza() != null) esistente.setCapienza(datiAggiornati.getCapienza());
            if (datiAggiornati.getGestoreId() != null) esistente.setGestoreId(datiAggiornati.getGestoreId());
            if (datiAggiornati.getIpAddressEdge() != null) esistente.setIpAddressEdge(datiAggiornati.getIpAddressEdge());

            localeRepository.save(esistente);
            return ResponseEntity.ok(toModel(esistente));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Rimuove un locale.
     *
     * @param id ID.
     * @return 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> elimina(@PathVariable("id") Long id) {
        if (localeRepository.existsById(id)) {
            localeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Utility method per convertire un Locale in EntityModel con _links
     * secondo le specifiche HATEOAS richieste.
     */
    private EntityModel<Locale> toModel(Locale locale) {
        EntityModel<Locale> model = EntityModel.of(locale);
        
        // Link a sé stesso
        model.add(linkTo(methodOn(AdminLocaleController.class).getAll()).slash(locale.getId()).withSelfRel());
        
        // Link al gestore se assegnato
        if (locale.getGestoreId() != null) {
            model.add(linkTo(UtenteController.class).slash(locale.getGestoreId()).withRel("gestore"));
        }
        
        // Link ai dispositivi attivi (usando Controller originale LocaleController come target simulato)
        model.add(linkTo(methodOn(LocaleController.class).getDispositiviLocale(locale.getId())).withRel("dispositivi"));

        return model;
    }
}