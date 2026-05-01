package com.bitpub.controllers;

import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.cloud.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST responsabile della gestione dei flussi di autenticazione per l'area amministrativa.
 * Espone gli endpoint necessari per la validazione delle credenziali e l'emissione dei token di sessione.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AdminAuthController {

    /** Provider per la logica di generazione e gestione dei JSON Web Token. */
    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * Elabora le richieste di accesso verificando le credenziali fornite nel corpo della richiesta.
     * In caso di successo, restituisce un oggetto {@link AuthResponse} contenente il JWT firmato.
     *
     * @param request Oggetto {@link AuthRequest} contenente email e password fornite dall'utente.
     * @return Una {@link ResponseEntity} contenente i dati di autenticazione o uno stato 401 in caso di fallimento.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        // Verifica puntuale delle credenziali amministrative (Logica di mockup per test di sistema)
        if ("admin@bitpub.it".equals(request.getEmail()) && "admin_password".equals(request.getPassword())) {

            // Generazione del token JWT con identificativo e ruolo "ADMIN"
            String token = tokenProvider.generateToken(request.getEmail(), "ADMIN");

            // Costruzione del DTO di risposta popolato con i metadati di sessione
            AuthResponse response = new AuthResponse("Admin", token, "ADMIN");

            // Ritorno dello stato 200 OK con il payload di autenticazione
            return ResponseEntity.ok(response);
        }

        // Restituzione dello stato HTTP 401 (Unauthorized) se la validazione fallisce
        return ResponseEntity.status(401).build();
    }
}
