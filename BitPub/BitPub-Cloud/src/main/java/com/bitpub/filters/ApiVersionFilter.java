package com.bitpub.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro per l'intercettazione e il controllo del Semantic Versioning.
 * Timothy: Gestione della logica di intercept.
 * Luca: Gestione della risposta di errore 406.
 */
@Component // Spring rileva automaticamente questo filtro all'avvio
public class ApiVersionFilter implements Filter {

    // La "parola d'ordine" esatta che la UI deve inviarci
    private static final String VERSIONE_RICHIESTA = "application/resources.v1+json";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Convertiamo le richieste in un formato più specifico per il Web (HTTP)
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Vogliamo controllare SOLO le rotte delle nostre API, ignorando il resto
        if (req.getRequestURI().startsWith("/api/")) {

            // TIMOTHY: Leggiamo l'Header "Accept" inviato dal Client
            String acceptHeader = req.getHeader("Accept");

            // Controllo la validità del Versioning
            if (acceptHeader == null || !acceptHeader.equals(VERSIONE_RICHIESTA)) {

                System.out.println("[API FILTER] Richiesta bloccata. Header Accept non valido: " + acceptHeader);

                // LUCA: Setup della risposta d'errore (406 Not Acceptable)
                res.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                res.setContentType("application/json");
                res.getWriter().write("{ \"errore\": \"Versione API non supportata. Utilizzare Accept: " + VERSIONE_RICHIESTA + "\" }");

                // Interrompiamo l'esecuzione qui, NON passiamo la palla al Controller di Stefano!
                return;
            }
        }

        // Se tutto è corretto (o se non è una rotta /api/), lasciamo passare la richiesta
        chain.doFilter(request, response);
    }
}