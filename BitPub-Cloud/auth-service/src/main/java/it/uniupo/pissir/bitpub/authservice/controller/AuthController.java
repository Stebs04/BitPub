/**
 * Autore: Luca Franzon 20054744
 * Controller REST che espone gli endpoint per la gestione dell'autenticazione.
 * Funge da punto di ingresso per i client che necessitano di accedere al sistema.
 */
package it.uniupo.pissir.bitpub.authservice.controller;

import it.uniupo.pissir.bitpub.authservice.dto.JwtResponse;
import it.uniupo.pissir.bitpub.authservice.dto.LoginRequest;
import it.uniupo.pissir.bitpub.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Gestisce la richiesta di accesso (login) di un utente.
     * Riceve le credenziali in formato JSON, ne verifica la validità formale
     * e demanda la logica applicativa al servizio di competenza.
     *
     * @param request l'oggetto contenente username e password
     * @return la risposta strutturata contenente il token JWT e i dati dell'utente
     */
    @PostMapping("/login")
    public JwtResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
