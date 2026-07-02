package it.uniupo.pissir.bitpub.gamecatalogservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Il ruolo effettivo (dal token JWT inoltrato dal gateway) viene stabilito dal
 * JwtAuthenticationFilter condiviso in bitpub-common, che popola il SecurityContext.
 * La catena HTTP resta aperta: l'accesso e' vincolato a livello di metodo con @PreAuthorize.
 * La scrittura del catalogo (create GameType / add SensorDefinition) e' riservata al GAME_ADMIN.
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
