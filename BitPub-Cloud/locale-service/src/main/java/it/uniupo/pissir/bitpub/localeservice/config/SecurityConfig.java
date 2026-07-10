package it.uniupo.pissir.bitpub.localeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Configurazione della sicurezza per il servizio.
 * Il ruolo effettivo dell'utente viene determinato dal JwtAuthenticationFilter, 
 * che analizza il token JWT inoltrato dal gateway. La catena di filtri HTTP rimane aperta; 
 * le restrizioni di accesso vengono applicate in modo selettivo sui singoli metodi tramite le annotazioni.
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
