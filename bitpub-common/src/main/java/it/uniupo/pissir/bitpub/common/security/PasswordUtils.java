package it.uniupo.pissir.bitpub.common.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtils {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        try {
            return encoder.matches(rawPassword, passwordHash);
        } catch (IllegalArgumentException e) {
            // Hash legacy/malformato (es. pre-BCrypt): considera la verifica fallita invece di propagare.
            return false;
        }
    }
}
