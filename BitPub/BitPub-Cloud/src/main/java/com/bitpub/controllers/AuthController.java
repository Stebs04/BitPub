package com.bitpub.controllers;

import com.bitpub.cloud.security.JwtUtil;
import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.models.Utente;
import com.bitpub.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller REST per l'autenticazione stateless.
 * Espone gli endpoint pubblici per la registrazione e il login.
 *
 * @author BitPub Team
 * @version 1.0
 */
@RestController
@RequestMapping(value = "/api/v1/auth", produces = "application/resources.v1+json")
public class AuthController {

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Registra un nuovo utente nel sistema con ruolo UTENTE_BASE.
     * La password viene hashata all'interno del costruttore di Utente.
     *
     * @param request body JSON con username, email, password
     * @return 201 Created con token e dettagli
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (request.getUsername() == null || request.getPassword() == null || request.getEmail() == null) {
            return ResponseEntity.badRequest().body("Dati mancanti");
        }

        if (utenteRepository.findByNickname(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username già in uso");
        }

        if (utenteRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email già in uso");
        }

        // Crea il nuovo utente. Costruttore: nickname, ruolo, nome, cognome, email, password
        // Nome e cognome non sono forniti in request, li lasciamo vuoti o usiamo username.
        Utente nuovoUtente = new Utente(request.getUsername(), "UTENTE_BASE", "", "", request.getEmail(), request.getPassword());
        utenteRepository.save(nuovoUtente);

        // Genera JWT
        String token = jwtUtil.generateToken(nuovoUtente.getNickname(), nuovoUtente.getRuolo());

        AuthResponse response = new AuthResponse(nuovoUtente.getNickname(), token, nuovoUtente.getRuolo());
        EntityModel<AuthResponse> model = EntityModel.of(response,
                linkTo(methodOn(UtenteController.class).getUtenteByNickname(nuovoUtente.getNickname(), null)).withRel("profilo")
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(model);
    }

    /**
     * Verifica le credenziali e restituisce un JWT stateless.
     *
     * @param request body JSON con username e password
     * @return 200 OK con token JWT
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().body("Dati mancanti");
        }

        Optional<Utente> utenteOpt = utenteRepository.findByNickname(request.getUsername());
        if (utenteOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali non valide");
        }

        Utente utente = utenteOpt.get();
        if (!utente.checkPassword(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali non valide");
        }

        // Genera JWT
        String token = jwtUtil.generateToken(utente.getNickname(), utente.getRuolo());

        AuthResponse response = new AuthResponse(utente.getNickname(), token, utente.getRuolo());
        EntityModel<AuthResponse> model = EntityModel.of(response,
                linkTo(methodOn(UtenteController.class).getUtenteByNickname(utente.getNickname(), null)).withRel("profilo")
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(model);
    }
}
