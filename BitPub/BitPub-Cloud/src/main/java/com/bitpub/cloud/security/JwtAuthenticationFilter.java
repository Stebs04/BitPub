package com.bitpub.cloud.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Filtro custom per l'autenticazione stateless basata su JWT.
 * Intercetta le richieste e valida il token presente nell'header Authorization.
 *
 * @author BitPub Team
 * @version 1.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;
        String role = null;

        // Estrai il token JWT dall'header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                if (jwtUtil.validateToken(jwt)) {
                    username = jwtUtil.extractUsername(jwt);
                    role = jwtUtil.extractRole(jwt);
                }
            } catch (Exception e) {
                // Token non valido o scaduto
            }
        }

        // Se l'utente è valido e non è già autenticato nel contesto di Spring Security
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // Creiamo un token di autenticazione di Spring Security
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
            
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Impostiamo l'autenticazione nel contesto di sicurezza
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        filterChain.doFilter(request, response);
    }
}
