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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // 1. Disabilitiamo il CSRF (Cross-Site Request Forgery).
                // È una protezione necessaria per le app basate sui Cookie,
                // ma inutile e problematica per le API REST Stateless.
                .csrf(csrf -> csrf.disable())

                // 2. Architettura 100% Stateless.
                // Spring non creerà MAI una sessione HTTP (niente JSESSIONID).
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. Regole di autorizzazione per le rotte (Endpoints).
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/error").permitAll()

                        // --- GESTIONE LOCALI (Admin) ---
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/locali").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/locali/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/locali").hasAnyRole("ADMIN", "GESTORE", "UTENTE")

                        // Solo l'Admin può vedere i log e lo stato della rete
                        .requestMatchers("/api/v1/system/**").hasRole("ADMIN")
                        // Solo l'Admin può forzare la chiusura delle sessioni
                        .requestMatchers("/api/v1/admin/sessions/**").hasRole("ADMIN")
                        // Gestione utenti (Ricerca e Sospensione)
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        // --- GESTIONE TORNEI (Gestore) ---
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/tornei").hasRole("GESTORE")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/tornei/**").hasRole("GESTORE")

                        // --- STATISTICHE TAVOLI ---
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/calciobalilla/stats").hasAnyRole("GESTORE", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/biliardo/stats").hasAnyRole("GESTORE", "ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/freccette/stats").hasAnyRole("GESTORE", "ADMIN")

                        // --- UTENTI ---
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/utenti/**").hasRole("UTENTE")

                        // Tutte le altre richieste richiedono autenticazione
                        .anyRequest().authenticated()
                )

                // 4. Aggiunta del filtro JWT custom prima del filtro di autenticazione standard
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}