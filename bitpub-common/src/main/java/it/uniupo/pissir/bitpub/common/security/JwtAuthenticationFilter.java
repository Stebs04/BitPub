/**
 * Autore: Luca Franzon 20054744
 *
 * Filtro per l'autenticazione basata su JWT. Si occupa di intercettare le richieste, 
 * estrarre il token e validarne l'autenticità prima di concedere l'accesso alle risorse.
 */
package it.uniupo.pissir.bitpub.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // Estrazione del token dalla richiesta corrente
            String jwt = parseJwt(request);
            
            // Verifichiamo la presenza e la validità del token prima di procedere
            if (jwt != null && jwtUtils.validateToken(jwt)) {
                // Recuperiamo i dati principali dell'utente direttamente dal payload del token
                String username = jwtUtils.getUsernameFromToken(jwt);
                String role = jwtUtils.getRoleFromToken(jwt);
                String userId = jwtUtils.getUserIdFromToken(jwt);
                String localeId = jwtUtils.getLocaleIdFromToken(jwt);

                // Aggiungiamo il prefisso standard richiesto da Spring Security per i ruoli
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                // Creiamo l'oggetto di autenticazione con le informazioni estratte
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(userId, username, role, localeId), null, Collections.singletonList(new SimpleGrantedAuthority(authority)));
                
                // Arricchiamo l'autenticazione con i dettagli specifici della richiesta web
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Impostiamo l'autenticazione nel contesto di sicurezza per renderla disponibile globalmente
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // In caso di errore durante la validazione logghiamo l'accaduto senza interrompere bruscamente l'esecuzione
            logger.error("Impossibile impostare l'autenticazione utente: {}", e);
        }

        // Passiamo il controllo al filtro successivo nella catena
        filterChain.doFilter(request, response);
    }

    // Metodo di supporto per isolare la logica di estrazione del token dall'header
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        // Controlliamo che l'header contenga del testo e inizi con il prefisso previsto
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
