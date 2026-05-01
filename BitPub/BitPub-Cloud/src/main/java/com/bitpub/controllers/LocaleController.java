package com.bitpub.controllers;

import com.bitpub.assembler.LocaleModelAssembler;
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
 * REST Controller per la gestione del ciclo di vita dell'entità {@link Locale}.
 * <p>
 * Espone le API per operazioni CRUD integrando il supporto HATEOAS. 
 * Utilizza un {@link LocaleModelAssembler} per la trasformazione delle risorse
 * e gestisce i vincoli di unicità (Nome, IP Edge) durante la persistenza.
 * </p>
 * * @author Stefano Bellan 20054330, Timothy Giolito 20054431
 */
@RestController
@RequestMapping(value = "/api/locali", produces = "application/resources.v1+json")
public class LocaleController {

    @Autowired
    private LocaleRepository localeRepository;

    @Autowired
    private LocaleModelAssembler assembler;

    /**
     * Recupera l'elenco completo dei locali registrati.
     * * @return {@link CollectionModel} contenente i locali arricchiti con link HATEOAS.
     */
    @GetMapping
    public CollectionModel<EntityModel<Locale>> getAllLocali() {
        List<EntityModel<Locale>> localiModel = localeRepository.findAll().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return CollectionModel.of(localiModel,
                linkTo(methodOn(LocaleController.class).getAllLocali()).withSelfRel()
        );
    }

    /**
     * Ricerca un locale tramite il suo identificativo univoco.
     * * @param id ID del locale.
     * @return 200 OK con il modello del locale, o 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<Locale>> getById(@PathVariable("id") Long id) {
        return localeRepository.findById(id)
                .map(l -> ResponseEntity.ok(assembler.toModel(l)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Ricerca un locale per nome (Unique Constraint).
     * * @param nome Nome del locale da cercare.
     * @return Modello della risorsa trovata.
     */
    @GetMapping("/nome/{nome}")
    public ResponseEntity<EntityModel<Locale>> getByNome(@PathVariable("nome") String nome) {
        return localeRepository.findByName(nome)
                .map(l -> ResponseEntity.ok(assembler.toModel(l)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Registra un nuovo locale nel sistema.
     * <p>
     * Verifica preventivamente l'unicità del nome e dell'indirizzo IP dell'Edge Node
     * per prevenire conflitti di rete o di dominio.
     * </p>
     * * @param nuovo Dati del nuovo locale.
     * @return 201 Created se l'operazione riesce, 409 Conflict se i vincoli sono violati.
     */
    @PostMapping
    public ResponseEntity<?> creaLocale(@RequestBody Locale nuovo) {
        if (nuovo.getIpAddressEdge() == null || nuovo.getIpAddressEdge().isEmpty()) {
            nuovo.setIpAddressEdge("127.0.0.1"); // Assegna IP di default se non fornito
        }
        
        if (localeRepository.existsByName(nuovo.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Nome locale già esistente.");
        }
        if (localeRepository.existsByIpAddressEdge(nuovo.getIpAddressEdge())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("IP Edge già assegnato a un altro locale.");
        }
        
        Locale salvato = localeRepository.save(nuovo);
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.toModel(salvato));
    }

    /**
     * Aggiorna i dati di un locale esistente.
     * * @param id ID del locale da modificare.
     * @param datiAggiornati Nuovi attributi da applicare.
     * @return Modello aggiornato o 404 se non presente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> aggiornaLocale(@PathVariable("id") Long id, @RequestBody Locale datiAggiornati) {
        return localeRepository.findById(id).map(esistente -> {
            esistente.setName(datiAggiornati.getName());
            esistente.setIpAddressEdge(datiAggiornati.getIpAddressEdge());
            localeRepository.save(esistente);
            return ResponseEntity.ok(assembler.toModel(esistente));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Rimuove un locale dal sistema.
     * * @param id ID della risorsa da eliminare.
     * @return 204 No Content in caso di successo, 404 se l'ID è inesistente.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminaLocale(@PathVariable("id") Long id) {
        if (localeRepository.existsById(id)) {
            localeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Recupera l'elenco dei dispositivi associati a un locale.
     * <p>
     * Nota: Questo endpoint funge da "Link Target" per la relazione HATEOAS 'dispositivi'.
     * </p>
     */
    @GetMapping("/{id}/dispositivi")
    public ResponseEntity<String> getDispositiviLocale(@PathVariable("id") Long id) {
        return ResponseEntity.ok("Dispositivi per il locale: " + id);
    }
}