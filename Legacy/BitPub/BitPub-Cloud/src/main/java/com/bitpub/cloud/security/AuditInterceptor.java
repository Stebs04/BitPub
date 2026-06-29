package com.bitpub.cloud.security;

import com.bitpub.models.AuditLogEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercettore per la registrazione automatica delle attività (Audit Trail).
 * Monitora le richieste HTTP in uscita e persiste i dettagli delle operazioni
 * nel database PostgreSQL per scopi di monitoraggio e compliance.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    /** Publisher per generare eventi applicativi in modo asincrono. */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Esegue la logica di logging dopo il completamento della richiesta HTTP.
     * Cattura metadati quali utente, URI, metodo HTTP e stato della risposta.
     *
     * @param request  L'oggetto {@link HttpServletRequest} della chiamata corrente.
     * @param response L'oggetto {@link HttpServletResponse} generato dal server.
     * @param handler  L'oggetto che ha gestito la richiesta.
     * @param ex       Eventuale eccezione sollevata durante l'elaborazione.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Inizializzazione di una nuova entità di log
        AuditLogEntity log = new AuditLogEntity();

        // 1. Estrazione dello username dal contesto di sicurezza gestito dal filtro JWT
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "ANONYMOUS";

        // 2. Popolamento dei dettagli descrittivi della transazione
        log.setSource("CLOUD"); // Identifica il Cloud come origine dell'evento
        log.setAction(request.getMethod()); // Metodo HTTP (GET, POST, etc.) utilizzato come azione
        log.setMessage("Richiesta a: " + request.getRequestURI() + " - Utente: " + username + " - Status: " + response.getStatus());

        // Definizione della severità del log in base al codice di stato HTTP
        if (response.getStatus() >= 400) {
            log.setLevel("ERROR"); // Codici 4xx e 5xx vengono trattati come errori
        } else {
            log.setLevel("INFO"); // Codici di successo o redirect
        }

        // 3. Pubblicazione asincrona dell'evento per evitare blocchi lato HTTP
        eventPublisher.publishEvent(new AuditApplicationEvent(this, log));
    }
}
