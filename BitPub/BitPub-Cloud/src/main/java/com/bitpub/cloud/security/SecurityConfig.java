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

                // 2. IL CUORE DEL TUO COMPITO: Architettura 100% Stateless.
                // Diciamo a Spring di non creare MAI una sessione HTTP (niente JSESSIONID).
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. Regole di autorizzazione per le rotte (Endpoints).
                // Usiamo hasAnyAuthority invece di hasAnyRole per evitare problemi con il prefisso "ROLE_"
                .authorizeHttpRequests(auth -> auth
                        // --- Sblocco richieste di pre-flight (OPTIONS) spesso causa di falsi errori 403 ---
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        
                        .requestMatchers("/api/v1/auth/**", "/error").permitAll() // Auth pubblica
                        
                        // --- GESTIONE LOCALI DASHBOARD ADMIN (Aggiunta regola per controller admin) ---
                        // Permettiamo l'accesso all'endpoint specifico per gli amministratori
                        .requestMatchers("/api/v1/admin/locali", "/api/v1/admin/locali/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                        
                        // --- GESTIONE LOCALI BASE (Gestore e Utente Base) ---
                        // Copriamo i percorsi base e i loro sotto-percorsi usando "/**"
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/locali", "/api/locali/**").hasAnyAuthority("ADMIN", "GESTORE", "ROLE_ADMIN", "ROLE_GESTORE")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/locali", "/api/locali/**").hasAnyAuthority("ADMIN", "GESTORE", "ROLE_ADMIN", "ROLE_GESTORE")
                        // Qui permettiamo all'UTENTE_BASE di fare chiamate GET per leggere i locali
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/locali", "/api/locali/**").hasAnyAuthority("ADMIN", "GESTORE", "UTENTE_BASE", "utente_base", "ROLE_ADMIN", "ROLE_GESTORE", "ROLE_UTENTE_BASE", "ROLE_utente_base")
                        
                        // --- GESTIONE TORNEI (Gestore) ---
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/tornei", "/api/tornei/**").hasAnyAuthority("GESTORE", "ROLE_GESTORE")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/tornei", "/api/tornei/**").hasAnyAuthority("GESTORE", "ROLE_GESTORE")
                        
                        // --- STATISTICHE TAVOLI E PARTITE ---
                        // Permettiamo la visualizzazione delle statistiche anche agli utenti base
                        // Rimosso il vincolo esplicito di HttpMethod per prevenire blocchi imprevisti ed esteso il pattern match
                        .requestMatchers("/api/calciobalilla", "/api/calciobalilla/**").hasAnyAuthority("GESTORE", "ADMIN", "UTENTE_BASE", "utente_base", "ROLE_GESTORE", "ROLE_ADMIN", "ROLE_UTENTE_BASE", "ROLE_utente_base")
                        .requestMatchers("/api/biliardo", "/api/biliardo/**").hasAnyAuthority("GESTORE", "ADMIN", "UTENTE_BASE", "utente_base", "ROLE_GESTORE", "ROLE_ADMIN", "ROLE_UTENTE_BASE", "ROLE_utente_base")
                        .requestMatchers("/api/statistiche/freccette", "/api/statistiche/freccette/**").hasAnyAuthority("GESTORE", "ADMIN", "UTENTE_BASE", "utente_base", "ROLE_GESTORE", "ROLE_ADMIN", "ROLE_UTENTE_BASE", "ROLE_utente_base")
                        
                        // --- UTENTI ---
                        // Un utente base può modificare il proprio profilo
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/utenti/**").hasAnyAuthority("UTENTE_BASE", "utente_base", "ROLE_UTENTE_BASE", "ROLE_utente_base") 
                        
                        // Per le altre API originali le proteggiamo richiedendo autenticazione generica
                        .anyRequest().authenticated()
                )
                
                // 4. Aggiunta del filtro JWT custom prima del filtro di autenticazione standard
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}