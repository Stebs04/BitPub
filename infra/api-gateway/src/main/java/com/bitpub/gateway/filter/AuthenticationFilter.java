package com.bitpub.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                throw new RuntimeException("Missing authorization information");
            }

            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            }

            try {
                SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
                Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(authHeader).getPayload();
                
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                String jwtClaimsJson = String.format("{\"username\":\"%s\",\"role\":\"%s\"}", username, role);
                String encodedClaims = java.net.URLEncoder.encode(jwtClaimsJson, java.nio.charset.StandardCharsets.UTF_8);

                // Add extracted user info to headers for downstream microservices
                ServerHttpRequest request = exchange.getRequest().mutate()
                        .header("X-Auth-User", encodedClaims)
                        .header("X-User-Id", username)
                        .header("X-User-Roles", role)
                        .build();
                        
                return chain.filter(exchange.mutate().request(request).build());
            } catch (Exception e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    public static class Config {
    }
}
