package com.bitpub.stats.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final GatewayAuthenticationFilter filter;

    public SecurityConfig(GatewayAuthenticationFilter filter) {
        this.filter = filter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/swagger-ui.html")).permitAll()
                .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/actuator/**")).permitAll()
                .requestMatchers(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/v1/stats/**")).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
