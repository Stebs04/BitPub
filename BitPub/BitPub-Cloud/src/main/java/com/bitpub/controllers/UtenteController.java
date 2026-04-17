package com.bitpub.controllers;

import com.bitpub.models.Utente;
import com.bitpub.repository.UtenteRepository;
import com.bitpub.utils.HateoasResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller REST per la gestione degli Utenti.
 * Espone le risorse del database PostgreSQL tramite endpoint State-Less.
 * @author Stefano Bellan 20054330
 */
@RestController
@RequestMapping("/api/utenti")
public class UtenteController {

    @Autowired
    private UtenteRepository utenteRepository;

    /**
     * Recupera un utente tramite il nickname unico.
     */
    @GetMapping("/{nickname}")
    public ResponseEntity<HateoasResource<Utente>> getUtenteByNickname(
            @PathVariable String nickname,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return utenteRepository.findByNickname(nickname)
                .map(utente -> ResponseEntity.ok(creaRisorsaConLink(utente)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recupera un utente tramite l'indirizzo email unico.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<HateoasResource<Utente>> getUtenteByEmail(
            @PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        return utenteRepository.findByEmail(email)
                .map(utente -> ResponseEntity.ok(creaRisorsaConLink(utente)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recupera la lista di utenti filtrata per ruolo (es. ADMIN, PLAYER).
     */
    @GetMapping("/ruolo/{ruolo}")
    public ResponseEntity<List<HateoasResource<Utente>>> getUtentiByRuolo(
            @PathVariable String ruolo,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        List<Utente> utenti = utenteRepository.findByRuolo(ruolo);

        List<HateoasResource<Utente>> risorse = utenti.stream()
                .map(this::creaRisorsaConLink)
                .collect(Collectors.toList());

        return ResponseEntity.ok(risorse);
    }

    /**
     * Ricerca globale per parola chiave su nome e cognome.
     */
    @GetMapping("/cerca")
    public ResponseEntity<List<HateoasResource<Utente>>> cercaUtenti(
            @RequestParam String keyword,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        List<Utente> risultati = utenteRepository.cercaPerNomeOCognome(keyword);

        List<HateoasResource<Utente>> risorse = risultati.stream()
                .map(this::creaRisorsaConLink)
                .collect(Collectors.toList());

        return ResponseEntity.ok(risorse);
    }

    /**
     * Metodo helper per iniettare i link HATEOAS in modo centralizzato.
     */
    private HateoasResource<Utente> creaRisorsaConLink(Utente utente) {
        HateoasResource<Utente> risorsa = new HateoasResource<>(utente);
        String nick = utente.getNickname();

        risorsa.addLink("self", "/api/utenti/" + nick);
        risorsa.addLink("modifica", "/api/utenti/" + nick);
        risorsa.addLink("partite", "/api/utenti/" + nick + "/partite");
        risorsa.addLink("dashboard_statistiche", "/api/statistiche/giocatore/" + nick);

        return risorsa;
    }
}