package com.bitpub.services;

import com.bitpub.cloud.security.JwtService;
import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.bitpub.models.Utente;
import com.bitpub.models.RegisterRequest;
import java.util.stream.Collectors;

/**
 * AuthService - Servizio centralizzato per la gestione dell'autenticazione.
 * * Nota Architetturale:
 * Questo servizio astrae la complessità dell'autenticazione dai controller.
 * Gestisce l'integrazione con Spring Security e la generazione del token ruolizzato,
 * garantendo un singolo punto di verità per le procedure di login del sistema.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UtenteRepository utenteRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(AuthenticationManager authenticationManager, 
                       JwtService jwtService, 
                       UtenteRepository utenteRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.utenteRepository = utenteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Esegue l'autenticazione dell'utente e genera un token JWT contenente i ruoli.
     * * @param request DTO contenente username e password.
     * @return AuthResponse contenente il token e i dettagli base dell'utente.
     */
    public AuthResponse authenticate(AuthRequest request) {
        // Validazione credenziali tramite l'AuthenticationManager di Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // Estrazione dei ruoli per includerli nel payload del token
        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // Generazione del JWT tramite il servizio dedicato
        String token = jwtService.generateToken(userDetails.getUsername(), roles);

        AuthResponse response = new AuthResponse(userDetails.getUsername(), token, roles);
        // Aggiunta link HATEOAS
        response.addLink("self", "/api/v1/utenti/" + userDetails.getUsername());
        response.addLink("locali", "/api/v1/locali");
        return response;
    }

    /**
     * Registra un nuovo utente nel sistema con ruolo UTENTE_BASE di default.
     * @param request DTO contenente i dati di registrazione.
     * @return AuthResponse contenente i dettagli (senza token per policy di sicurezza, o con token se richiesto).
     */
    public AuthResponse register(RegisterRequest request) {
        if (utenteRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username già in uso");
        }
        
        Utente nuovoUtente = new Utente();
        nuovoUtente.setUsername(request.getUsername());
        nuovoUtente.setEmail(request.getEmail());
        nuovoUtente.setPassword(passwordEncoder.encode(request.getPassword()));
        nuovoUtente.setRole("UTENTE_BASE");
        nuovoUtente.setAttivo(true);
        nuovoUtente.setCredito(0.0);
        
        utenteRepository.save(nuovoUtente);
        
        AuthResponse response = new AuthResponse(nuovoUtente.getUsername(), null, "UTENTE_BASE");
        response.addLink("self", "/api/v1/utenti/" + nuovoUtente.getUsername());
        response.addLink("login", "/api/v1/auth/login");
        return response;
    }
}