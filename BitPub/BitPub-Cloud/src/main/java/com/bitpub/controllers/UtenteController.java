package com.bitpub.controllers;

import com.bitpub.assembler.UtenteModelAssembler;
import com.bitpub.models.Utente;
import com.bitpub.repository.UtenteRepository;
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
 * Controller REST per la gestione dell'anagrafica e delle attività degli Utenti.
 * <p>
 * Espone le risorse del database PostgreSQL adottando un paradigma stateless.
 * La navigazione tra il profilo, le statistiche e lo storico partite è garantita 
 * dall'integrazione del supporto HATEOAS tramite {@link UtenteModelAssembler}.
 * </p>
 * 
 * @author Stefano Bellan 20054330
 */
@RestController
@RequestMapping(value = "/api/utenti", produces = "application/resources.v1+json")
public class UtenteController {

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private UtenteModelAssembler assembler;

    /**
     * Recupera il profilo utente partendo dal nickname (identificatore naturale).
     * 
     * @param nickname   Nome utente univoco.
     * @param authHeader Token di autorizzazione (opzionale per consultazione pubblica).
     * @return 200 OK con il modello utente, o 404 Not Found se il nickname non esiste.
     */
    @GetMapping("/{nickname}")
    public ResponseEntity<EntityModel<Utente>> getUtenteByNickname(
            @PathVariable String nickname,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return utenteRepository.findByNickname(nickname)
                .map(utente -> ResponseEntity.ok(assembler.toModel(utente)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Ricerca un utente tramite l'indirizzo email associato.
     * 
     * @param email      Indirizzo email dell'utente.
     * @param authHeader Header di sicurezza.
     * @return Modello della risorsa {@link Utente}.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<EntityModel<Utente>> getUtenteByEmail(
            @PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return utenteRepository.findByEmail(email)
                .map(utente -> ResponseEntity.ok(assembler.toModel(utente)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Filtra gli utenti in base al ruolo assegnato (es. 'ADMIN', 'PLAYER').
     * 
     * @param ruolo      Tag del ruolo da filtrare.
     * @param authHeader Header di sicurezza.
     * @return {@link CollectionModel} degli utenti corrispondenti al criterio.
     */
    @GetMapping("/ruolo/{ruolo}")
    public ResponseEntity<CollectionModel<EntityModel<Utente>>> getUtentiByRuolo(
            @PathVariable String ruolo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        List<Utente> utenti = utenteRepository.findByRuolo(ruolo);
        List<EntityModel<Utente>> risorse = utenti.stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(risorse,
                linkTo(methodOn(UtenteController.class).getUtentiByRuolo(ruolo, authHeader)).withSelfRel()));
    }

    /**
     * Esegue una ricerca full-text o parziale su nome e cognome.
     * 
     * @param keyword    Stringa di ricerca.
     * @param authHeader Header di sicurezza.
     * @return Collezione di risultati filtrati.
     */
    @GetMapping("/cerca")
    public ResponseEntity<CollectionModel<EntityModel<Utente>>> cercaUtenti(
            @RequestParam String keyword,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        List<Utente> risultati = utenteRepository.cercaPerNomeOCognome(keyword);
        List<EntityModel<Utente>> risorse = risultati.stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(risorse,
                linkTo(methodOn(UtenteController.class).cercaUtenti(keyword, authHeader)).withSelfRel()));
    }

    /**
     * Endpoint per la modifica dei dati del profilo.
     * <p>Target del link relazionale 'modifica'.</p>
     */
    @PutMapping("/{nickname}")
    public ResponseEntity<String> modificaUtente(@PathVariable String nickname) {
        return ResponseEntity.ok("Endpoint modifica per " + nickname);
    }

    /**
     * Recupera lo storico delle partite giocate dall'utente.
     * <p>Target del link relazionale 'partite'.</p>
     */
    @GetMapping("/{nickname}/partite")
    public ResponseEntity<String> getPartiteUtente(@PathVariable String nickname) {
        return ResponseEntity.ok("Storico partite per " + nickname);
    }

    /**
     * Punto di accesso per le metriche di performance e ranking del giocatore.
     * <p>Target del link relazionale 'dashboard_statistiche'.</p>
     */
    @GetMapping("/statistiche/giocatore/{nickname}")
    public ResponseEntity<String> getStatisticheUtente(@PathVariable String nickname) {
        return ResponseEntity.ok("Dashboard statistiche per " + nickname);
    }
}
