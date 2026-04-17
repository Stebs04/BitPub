package com.bitpub.cloud.exception;

/**
 * Modello standard per i messaggi di errore delle API REST.
 */
public class ApiErrorResponse {
    private int status;
    private String errore;
    private String messaggio;

    public ApiErrorResponse(int status, String errore, String messaggio) {
        this.status = status;
        this.errore = errore;
        this.messaggio = messaggio;
    }

    // Getter e Setter necessari a Spring per convertire in JSON
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getErrore() { return errore; }
    public void setErrore(String errore) { this.errore = errore; }

    public String getMessaggio() { return messaggio; }
    public void setMessaggio(String messaggio) { this.messaggio = messaggio; }
}