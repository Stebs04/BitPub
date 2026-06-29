package com.bitpub.cloud.config;

import com.bitpub.models.Utente;
import com.bitpub.repository.UtenteRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DataInitializer - Si occupa di popolare i dati essenziali di sistema all'avvio.
 * Garantisce l'esistenza dell'utente ADMIN di default se non è già presente,
 * hashando correttamente la password con BCrypt come richiesto dalle policy di sicurezza.
 */
@Configuration
public class DataInitializer {

    @Bean
    public ApplicationRunner initDatabase(UtenteRepository utenteRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Verifica se esiste già un utente con ruolo ADMIN
            if (utenteRepository.findByRole(Utente.Ruolo.ADMIN).isEmpty()) {
                Utente admin = new Utente();
                admin.setUsername("admin");
                // Password in chiaro comunicata all'utente: BitPub@Admin2024!
                admin.setPassword(passwordEncoder.encode("BitPub@Admin2024!"));
                admin.setEmail("admin@bitpub.com");
                admin.setNome("Amministratore");
                admin.setCognome("Di Sistema");
                admin.setRole("ADMIN");
                admin.setAttivo(true);
                admin.setCredito(0.0);
                admin.setAnni(99);

                utenteRepository.save(admin);
                System.out.println("[SISTEMA] Utente ADMIN di default creato con successo.");
            }
        };
    }
}
