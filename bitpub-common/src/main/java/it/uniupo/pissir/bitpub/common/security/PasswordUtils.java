/**
 * Autore: Luca Franzon 20054744
 *
 * Componente per la gestione sicura delle password.
 * Implementa le funzionalità di hashing e verifica tramite l'algoritmo BCrypt.
 */
package it.uniupo.pissir.bitpub.common.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtils {

    // Utilizziamo l'implementazione standard di BCrypt fornita da Spring Security
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Funzione per generare l'hash di una password in chiaro
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // Funzione per confrontare una password in chiaro con il suo hash precedentemente salvato
    public boolean matches(String rawPassword, String passwordHash) {
        try {
            return encoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            // In caso di hash malformati, ad esempio per migrazioni da vecchi sistemi, 
            // assumiamo che la password sia errata senza bloccare l'applicazione
            return false;
        }
    }
}
