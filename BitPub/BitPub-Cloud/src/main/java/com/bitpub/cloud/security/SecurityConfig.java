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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/error").permitAll() // Auth pubblica
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN") // Solo admin
                        .requestMatchers("/api/v1/gestore/**").hasAnyRole("GESTORE", "ADMIN") // Gestore e admin
                        // Per le altre API originali le proteggiamo richiedendo autenticazione
                        .anyRequest().authenticated()
                )
                
                // 4. Aggiunta del filtro JWT custom prima del filtro di autenticazione standard
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}