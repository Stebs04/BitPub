package com.bitpub.controllers;

import com.bitpub.models.CalciobalillaStats;
import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.repository.PartitaCalciobalillaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// Import statici fondamentali per la generazione dinamica dei link
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.models.Utente;
import com.bitpub.repository.AuditLogEntity;
import com.bitpub.repository.AuditLogRepository;
import com.bitpub.repository.PartitaCalciobalillaRepository;
import com.bitpub.repository.UtenteRepository;
import com.bitpub.services.PersistenceService;
import com.bitpub.mqtt.CloudMqttGateway;
import com.bitpub.utils.MqttCalciobalillaTopics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller per la gestione delle risorse relative alle partite di calciobalilla.
 * <p>
 * Implementa il livello HATEOAS esponendo i metodi per recuperare tutte le partite,
 * ottenere i dettagli di una singola partita e aggregare le statistiche generali.
 * I risultati sono incapsulati in {@code CollectionModel} e {@code EntityModel}.
 * </p>
 *
 * @author Stefano Bellan (Implementazione Core e Logica di Dominio)
 */
@RestController
@RequestMapping(value = "/api/calciobalilla")
@CrossOrigin(origins = "*") // Risolve eventuali conflitti di permessi legati alle policy CORS
public class CalciobalillaController {
    @Autowired
    private PartitaCalciobalillaRepository repository;

    /**
     * Recupera l'elenco completo delle partite di calciobalilla.
     * <p>
     * Ogni entità restituita è arricchita con link HATEOAS per la navigazione
     * verso la singola partita, lo storico, e le informazioni del torneo associato (se presente).
     * </p>
     *
     * @return un {@link ResponseEntity} contenente un {@link CollectionModel} di partite
     */
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<PartitaCalciobalilla>>> getAllPartite() {
        List<EntityModel<PartitaCalciobalilla>> partite = repository.findAll().stream()
                .map(this::aggiungiLinkHateoas)
                .collect(Collectors.toList());

        // 1. Creiamo il CollectionModel avvolgendo la lista
        CollectionModel<EntityModel<PartitaCalciobalilla>> collectionModel = CollectionModel.of(partite);

        // 2. Aggiungiamo un link "self" alla collezione stessa
        collectionModel.add(linkTo(methodOn(CalciobalillaController.class).getAllPartite()).withSelfRel());

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Recupera una singola partita per ID con link ipertestuali dinamici.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PartitaCalciobalilla>> getPartitaById(@PathVariable("id") Long id) {
        return repository.findById(id)
                .map(partita -> ResponseEntity.ok(aggiungiLinkHateoas(partita)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint per le statistiche aggregate.
     */
    @GetMapping("/stats")
    public ResponseEntity<CalciobalillaStats> getGlobalStats() {
        int rullate = repository.findAll().stream()
                .mapToInt(PartitaCalciobalilla::getTotaleRullate)
                .sum();
        int vRossi = repository.countVittorieRossi();
        int vBlu = repository.countVittorieBlu();

        return ResponseEntity.ok(new CalciobalillaStats(rullate, vRossi, vBlu));
    }

    /**
     * Arricchisce l'entità PartitaCalciobalilla con i metadati ipertestuali (Fase
     * 22).
     */
    private EntityModel<PartitaCalciobalilla> aggiungiLinkHateoas(PartitaCalciobalilla partita) {
        EntityModel<PartitaCalciobalilla> risorsa = EntityModel.of(partita);

        // Link "self": punta alla risorsa corrente
        risorsa.add(linkTo(methodOn(CalciobalillaController.class).getPartitaById(partita.getId())).withSelfRel());

        // Link "storico": punta all'elenco completo
        risorsa.add(linkTo(methodOn(CalciobalillaController.class).getAllPartite()).withRel("storico"));

        // Link al Torneo: assicura il salto tra moduli in modo dinamico
        if (partita.getTorneo() != null) {
            risorsa.add(linkTo(methodOn(TorneoController.class).getTorneoById(partita.getTorneo().getId()))
                    .withRel("dettagli_torneo"));
        }

        return risorsa;
    }
}