package com.bitpub.cloud.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configurazione di sicurezza per il Cloud BitPub.
 * Obiettivo: Garantire un'architettura API REST rigorosamente State-Less.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disabilitiamo il CSRF (Cross-Site Request Forgery).
            // Nelle architetture Stateless basate su JWT, il token stesso protegge da questa vulnerabilità.
            .csrf(csrf -> csrf.disable())

            // 2. Configurazione CORS (Cross-Origin Resource Sharing).
            // Essenziale per permettere al Frontend (es. Angular/React) di comunicare con il Backend.
            .cors(cors -> cors.configure(http))

            // 3. Politica di gestione della Sessione.
            // Confermiamo l'approccio 100% Stateless: Spring Security non creerà mai una sessione server-side.
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 4. Definizione del perimetro di accesso (RBAC - Role Based Access Control).
            .authorizeHttpRequests(auth -> auth
                // Rotte pubbliche: accesso libero per autenticazione e registrazione.
                .requestMatchers("/api/v1/auth/**").permitAll()

                // Servizi di Gioco: Accessibili a tutti i livelli di utenza registrata.
                // Usiamo hasAnyAuthority per mappare esattamente i permessi senza prefissi nascosti.
                .requestMatchers("/api/v1/calciobalilla/**").hasAnyAuthority("UTENTE_BASE", "GESTORE", "ADMIN")
                .requestMatchers("/api/v1/tornei/**").hasAnyAuthority("UTENTE_BASE", "GESTORE", "ADMIN")
                .requestMatchers("/api/v1/biliardo/**").hasAnyAuthority("UTENTE_BASE", "GESTORE", "ADMIN")
                .requestMatchers("/api/v1/freccette/**").hasAnyAuthority("UTENTE_BASE", "GESTORE", "ADMIN")

                // Rotte Amministrative: Riservate esclusivamente ai ruoli specifici.
                .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/gestore/**").hasAuthority("GESTORE")

                // Chiusura di sicurezza: ogni altra rotta non esplicitata richiede autenticazione.
                .anyRequest().authenticated()
            )

            // 5. Chain dei filtri personalizzata.
            // Inseriamo il controllo del JWT prima del filtro di autenticazione standard di Spring.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
