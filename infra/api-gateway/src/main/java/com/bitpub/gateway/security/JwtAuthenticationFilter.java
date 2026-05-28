package com.bitpub.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter implements WebFilter {

    @Value("${jwt.secret:default-secret-key-for-development-must-be-long-enough}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String username = claims.getSubject();
                if (username != null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.emptyList()
                    );

                    // Relay the token downstream, potentially modify headers
                    java.util.Map<String, Object> claimsMap = new java.util.HashMap<>();
                    claimsMap.put("userId", claims.get("userId"));
                    claimsMap.put("username", username);
                    claimsMap.put("role", claims.get("role"));
                    claimsMap.put("permissions", claims.get("permissions"));
                    claimsMap.put("localeIds", claims.get("localeIds"));
                    claimsMap.put("tokenVersion", claims.get("tokenVersion"));
                    claimsMap.put("traceId", claims.get("traceId"));
                    
                    String encodedClaims = "";
                    try {
                        String claimsJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(claimsMap);
                        encodedClaims = java.net.URLEncoder.encode(claimsJson, java.nio.charset.StandardCharsets.UTF_8.toString());
                    } catch (Exception ex) {
                        // Ignore mapping error
                    }

                    final String finalEncodedClaims = encodedClaims;
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .headers(h -> h.set("X-Auth-User", finalEncodedClaims))
                            .build();
                    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

                    return chain.filter(mutatedExchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                }
            } catch (Exception e) {
                // Invalid token, do not set authentication
            }
        }

        return chain.filter(exchange);
    }
}
