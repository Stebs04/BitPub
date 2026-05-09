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

    @Autowired
    public AuthService(AuthenticationManager authenticationManager, 
                       JwtService jwtService, 
                       UtenteRepository utenteRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.utenteRepository = utenteRepository;
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

        return new AuthResponse(token, userDetails.getUsername(), roles);
    }
}