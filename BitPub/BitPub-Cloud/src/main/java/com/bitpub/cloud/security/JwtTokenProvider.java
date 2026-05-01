package com.bitpub.cloud.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * Classe responsabile della creazione e validazione dei token JWT.
 */
@Component
public class JwtTokenProvider {

    // Chiave segreta per firmare il token (in produzione andrebbe in un file esterno sicuro)
    private final String SECRET_KEY = "BitPub_Super_Secret_Key_Per_Admin_Flow";

    // Durata del token: 24 ore (espressa in millisecondi)
    private final long VALIDITY = 86400000;

    /**
     * Crea un token JWT contenente lo username e il ruolo.
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role) // Inseriamo il ruolo dentro il token
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + VALIDITY))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
}