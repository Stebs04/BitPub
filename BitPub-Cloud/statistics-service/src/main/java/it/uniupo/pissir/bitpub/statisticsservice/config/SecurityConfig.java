/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configurazione del livello di sicurezza dell'applicazione.
 * L'estrazione e validazione del ruolo dell'utente avvengono tramite il JwtAuthenticationFilter condiviso,
 * il quale legge le intestazioni fornite dal gateway (X-User-Id e X-User-Role) per popolare il contesto di sicurezza.
 * La SecurityFilterChain di base è configurata per permettere il traffico in ingresso, demandando i
 * controlli autorizzativi di grana fine alle annotazioni @PreAuthorize poste sui singoli metodi.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
