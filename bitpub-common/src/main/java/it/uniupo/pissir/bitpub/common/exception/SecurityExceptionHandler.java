/**
 * Autore: Luca Franzon 20054744
 *
 * Gestore globale per le eccezioni legate alla sicurezza.
 * Traduce gli errori interni di autorizzazione e autenticazione in risposte HTTP strutturate.
 */
package it.uniupo.pissir.bitpub.common.exception;

import it.uniupo.pissir.bitpub.common.dto.ErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
@ConditionalOnClass(name = "org.springframework.security.access.AccessDeniedException")
public class SecurityExceptionHandler {

    // Intercetta i tentativi di accesso a risorse per le quali l'utente non possiede il ruolo necessario
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        // Costruiamo una risposta d'errore standardizzata per il client
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Accesso negato: permessi insufficienti per questa operazione")
                .path("Sconosciuto")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Intercetta i problemi legati alla mancata autenticazione o a credenziali errate
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        // Prepariamo la risposta indicando chiaramente la necessità di autenticarsi
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Autenticazione richiesta: credenziali mancanti o non valide")
                .path("Sconosciuto")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
}
