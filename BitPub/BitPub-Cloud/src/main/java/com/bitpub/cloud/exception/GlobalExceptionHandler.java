package com.bitpub.cloud.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Intercettore globale per la gestione centralizzata delle eccezioni REST.
 * Traduce le eccezioni interne di Spring e JPA nel formato standard ApiErrorResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Semantic Versioning (Pre-esistente)
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiErrorResponse> gestisciNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        ApiErrorResponse errore = new ApiErrorResponse(
                HttpStatus.NOT_ACCEPTABLE.value(),
                "Not Acceptable - Versione API non supportata",
                "Devi specificare la versione dell'API nell'header. Usa: 'Accept: application/resources.v1+json'"
        );
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(errore);
    }

    // 2. Risorsa Non Trovata (Es. utente o torneo non esistente)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        ApiErrorResponse errore = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Risorsa Non Trovata",
                ex.getMessage() != null ? ex.getMessage() : "La risorsa richiesta non è presente nel database."
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errore);
    }

    // 3. Conflitto di Concorrenza (Es. due amministratori modificano lo stesso locale)
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockingFailure(OptimisticLockException ex) {
        ApiErrorResponse errore = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflitto di Concorrenza",
                "La risorsa è stata appena modificata da un altro utente. Ricarica i dati e riprova."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errore);
    }

    // 4. Errore Validazione Payload (Es. email malformata o campo vuoto)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Unisce tutti gli errori di validazione dei campi in una singola stringa leggibile
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ApiErrorResponse errore = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Errore di Validazione Dati",
                errorMessage
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errore);
    }

    // 5. Fallback Globale (Protezione contro stack trace esposti)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        // Loggare l'errore reale nel server (importante per il debug interno)
        System.err.println("[CRITICAL ERROR] Errore imprevisto non gestito: " + ex.getMessage());

        ApiErrorResponse errore = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Errore Interno del Server",
                "Si è verificato un errore imprevisto. Riprova più tardi o contatta il supporto."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errore);
    }
}