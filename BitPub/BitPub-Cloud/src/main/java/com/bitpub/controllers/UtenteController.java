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
 * * @author Stefano Bellan 20054330
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
     * * @param nickname   Nome utente univoco.
     * @param authHeader Token di autorizzazione (opzionale per consultazione pubblica).
     * @return ResponseEntity con status 200 OK e il modello utente HATEOAS, o 404 Not Found.
     */
    @GetMapping("/{nickname}")
    public ResponseEntity<EntityModel<Utente>> getUtenteByNickname(
            @PathVariable("nickname") String nickname,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return utenteRepository.findByNickname(nickname)
                .map(utente -> ResponseEntity.ok(assembler.toModel(utente)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Ricerca un utente tramite l'indirizzo email associato.
     * * @param email      Indirizzo email dell'utente.
     * @param authHeader Header di sicurezza.
     * @return ResponseEntity con il modello della risorsa {@link Utente} se presente.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<EntityModel<Utente>> getUtenteByEmail(
            @PathVariable("email") String email,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return utenteRepository.findByEmail(email)
                .map(utente -> ResponseEntity.ok(assembler.toModel(utente)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Filtra gli utenti in base al ruolo assegnato (es. 'ADMIN', 'PLAYER').
     * * @param ruolo      Tag del ruolo da filtrare.
     * @param authHeader Header di sicurezza.
     * @return ResponseEntity contenente la {@link CollectionModel} degli utenti filtrati.
     */
    @GetMapping("/ruolo/{ruolo}")
    public ResponseEntity<CollectionModel<EntityModel<Utente>>> getUtentiByRuolo(
            @PathVariable("ruolo") String ruolo,
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
     * * @param keyword    Stringa di ricerca.
     * @param authHeader Header di sicurezza.
     * @return ResponseEntity contenente la collezione HATEOAS di risultati.
     */
    @GetMapping("/cerca")
    public ResponseEntity<CollectionModel<EntityModel<Utente>>> cercaUtenti(
            @RequestParam("keyword") String keyword,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        List<Utente> risultati = utenteRepository.cercaPerNomeOCognome(keyword);
        List<EntityModel<Utente>> risorse = risultati.stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(risorse,
                linkTo(methodOn(UtenteController.class).cercaUtenti(keyword, authHeader)).withSelfRel()));
    }

    /**
     * Aggiorna o modifica parzialmente i dati del profilo di un utente.
     * * @param nickname Identificativo naturale dell'utente da modificare.
     * @return ResponseEntity con messaggio di conferma.
     */
    @PutMapping("/{nickname}")
    public ResponseEntity<?> modificaUtente(@PathVariable("nickname") String nickname, @RequestBody Utente datiAggiornati) {
        return utenteRepository.findByNickname(nickname)
                .map(utenteEsistente -> {
                    if (datiAggiornati.getEmail() != null) utenteEsistente.setEmail(datiAggiornati.getEmail());
                    // add other fields you allow to update? We can assume standard fields like password
                    if (datiAggiornati.getPassword() != null) utenteEsistente.setPassword(datiAggiornati.getPassword());
                    utenteRepository.save(utenteEsistente);
                    return ResponseEntity.ok(assembler.toModel(utenteEsistente));
                }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recupera lo storico delle partite giocate dall'utente richiesto.
     * * @param nickname Identificativo dell'utente di interesse.
     * @return ResponseEntity indicante l'esito della chiamata per lo storico.
     */
    @GetMapping("/{nickname}/partite")
    public ResponseEntity<String> getPartiteUtente(@PathVariable("nickname") String nickname) {
        return ResponseEntity.ok("Storico partite per " + nickname);
    }

    /**
     * Recupera le metriche di performance e il ranking del giocatore.
     * * @param nickname Identificativo dell'utente per cui visualizzare le statistiche.
     * @return ResponseEntity indicante l'esito della chiamata analitica.
     */
    @GetMapping("/statistiche/giocatore/{nickname}")
    public ResponseEntity<String> getStatisticheUtente(@PathVariable("nickname") String nickname) {
        return ResponseEntity.ok("Dashboard statistiche per " + nickname);
    }
}