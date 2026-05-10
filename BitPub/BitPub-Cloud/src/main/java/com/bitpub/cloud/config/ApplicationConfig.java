package com.bitpub.cloud.config;

import com.bitpub.repository.UtenteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configurazione centrale dell'infrastruttura di sicurezza e dei bean applicativi.
 * Gestisce l'integrazione tra il layer di persistenza (Repository) e il motore di autenticazione.
 *
 * @author Senior Software Engineer
 */
@Configuration
public class ApplicationConfig {

    private final UtenteRepository repository;

    /**
     * Dependency Injection tramite costruttore per garantire l'immutabilità
     * e facilitare l'injection nei test d'integrazione.
     */
    public ApplicationConfig(UtenteRepository repository) {
        this.repository = repository;
    }

    /**
     * Fornisce l'implementazione del servizio di caricamento utenti.
     * Grazie all'implementazione dell'interfaccia UserDetails da parte della classe Utente,
     * il mapping avviene automaticamente senza necessità di adapter esterni.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente con username " + username + " non trovato nel sistema."));
    }

    /**
     * Configura il provider di autenticazione standard utilizzando DAO.
     * Collega il codificatore delle password e il servizio utenti personalizzato.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Esone l'AuthenticationManager standard di Spring Security.
     * Fondamentale per il corretto funzionamento dei controller di autenticazione (es. login).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configura l'algoritmo di hashing BCrypt per la gestione sicura delle credenziali.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}