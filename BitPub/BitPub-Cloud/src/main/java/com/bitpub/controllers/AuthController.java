package com.bitpub.controllers;

import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.models.RegisterRequest;
import com.bitpub.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - Punto di accesso unico per l'autenticazione al sistema BitPub.
 * * Refactoring Senior Note:
 * È stata rimossa la duplicazione tra AdminAuthController e AuthController.
 * Il controller ora funge solo da interfaccia REST, delegando la logica di
 * emissione dei token e validazione al layer di servizio (AuthService).
 * Supporta nativamente tutti i ruoli definiti nel sistema.
 */
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Permette l'accesso da client JavaFX e Web
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint di login universale.
     * Gestisce l'autenticazione per Utenti, Gestori e Amministratori.
     * * @param request Credenziali fornite dal client.
     * @return ResponseEntity con il JWT e le informazioni di sessione.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        // La logica è centralizzata nel service per garantire coerenza
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + response.getToken())
                .body(response);
    }

    /**
     * Endpoint di registrazione universale.
     * Registra un nuovo utente con ruolo UTENTE_BASE.
     * @param request Credenziali per la registrazione (username, email, password).
     * @return ResponseEntity con i dettagli dell'utente registrato e link HATEOAS.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint diagnostico per la verifica dello stato del servizio di autenticazione.
     */
    @GetMapping("/status")
    public ResponseEntity<String> checkStatus() {
        return ResponseEntity.ok("Authentication Service is ONLINE");
    }
}