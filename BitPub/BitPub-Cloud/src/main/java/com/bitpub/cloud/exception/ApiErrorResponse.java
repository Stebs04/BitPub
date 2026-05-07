package com.bitpub.cloud.exception;

import java.util.UUID;

/**
 * Modello standard per i messaggi di errore delle API REST.
 * Include un correlationId per facilitare il tracciamento distribuito
 * e il debugging dei log in caso di fallimenti.
 */
public class ApiErrorResponse {
    private int status;
    private String errore;
    private String messaggio;

    // Nuovo campo critico per il tracciamento
    private String correlationId;

    /**
     * Costruttore standard (Retrocompatibile).
     * Genera automaticamente un Correlation ID univoco al momento dell'errore.
     */
    public ApiErrorResponse(int status, String errore, String messaggio) {
        this.status = status;
        this.errore = errore;
        this.messaggio = messaggio;
        this.correlationId = UUID.randomUUID().toString();
    }

    /**
     * Costruttore avanzato.
     * Permette di iniettare un Correlation ID esistente (es. propagato da un Header di tracciamento).
     */
    public ApiErrorResponse(int status, String errore, String messaggio, String correlationId) {
        this.status = status;
        this.errore = errore;
        this.messaggio = messaggio;
        this.correlationId = correlationId;
    }

    // --- Getter e Setter necessari a Spring per la conversione in JSON ---

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getErrore() { return errore; }
    public void setErrore(String errore) { this.errore = errore; }

    public String getMessaggio() { return messaggio; }
    public void setMessaggio(String messaggio) { this.messaggio = messaggio; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}