package com.bitpub.cloud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Intercettore globale per la gestione centralizzata delle eccezioni REST.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Cattura gli errori legati al Semantic Versioning (Header Accept errato o mancante).
     * @param ex l'eccezione generata da Spring
     * @return Risposta JSON con stato 406 Not Acceptable
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> gestisciNotAcceptable(HttpMediaTypeNotAcceptableException ex) {

        ApiErrorResponse errore = new ApiErrorResponse(
                HttpStatus.NOT_ACCEPTABLE.value(),
                "Not Acceptable - Versione API non supportata",
                "Devi specificare la versione dell'API nell'header. Usa: 'Accept: application/resources.v1+json'"
        );

        System.err.println("[Semantic Versioning] Rifiutata richiesta senza Header Accept valido.");

        // Restituisce il JSON e forza il codice HTTP a 406
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(errore);
    }
}