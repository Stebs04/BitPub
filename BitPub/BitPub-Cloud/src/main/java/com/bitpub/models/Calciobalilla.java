package com.bitpub.models;

/**
 * Importazione dell'annotazione Expose della libreria Google Gson
 * per il controllo della serializzazione/deserializzazione JSON.
 */
import com.google.gson.annotations.Expose;

/**
 * Rappresenta l'entità Calciobalilla per la gestione degli eventi di gioco.
 * Questa classe modella i dati provenienti dai dispositivi di monitoraggio.
 *
 * @author Stefano Bellan 20054330
 */
public class Calciobalilla {

    /** Identificativo univoco del dispositivo che ha generato l'evento. */
    @Expose
    private String idDispositivo;

    /** Marca temporale dell'evento espressa in formato Long (millisecondi). */
    @Expose
    private Long timestamp;

    /** Tipologia dell'evento registrato (es. "goal", "inizio_partita"). */
    @Expose
    private String tipoEvento;

    /** Nome o colore della squadra associata all'evento. */
    @Expose
    private String squadra;

    /** Durata della giocata con la pallina corrente espressa in secondi. */
    @Expose
    private int durataPallinaSecondi;

    /**
     * Costruttore parametrizzato per l'inizializzazione completa di un evento.
     * Effettua una validazione dei parametri di input.
     *
     * @param timestamp            Il momento in cui si è verificato l'evento.
     * @param tipoEvento           La descrizione del tipo di evento.
     * @param squadra              La squadra coinvolta.
     * @param durataPallinaSecondi Il tempo di possesso/gioco della pallina.
     * @throws IllegalArgumentException Se i parametri obbligatori sono nulli, vuoti o negativi.
     */
    public Calciobalilla(int idDispositivo, Long timestamp, String tipoEvento, String squadra, int durataPallinaSecondi) {
        // Verifica la validità dei dati in ingresso per garantire l'integrità dell'oggetto
        if(idDispositivo <= 0 || timestamp == null || tipoEvento == null || tipoEvento.isBlank() || squadra == null || squadra.isBlank() || durataPallinaSecondi < 0){
            // Solleva un'eccezione se i vincoli di business non sono rispettati
            throw new IllegalArgumentException("Assenza di alcuni campi durante l'inizializzazione!!");
        }
        this.timestamp = timestamp; // Assegna il timestamp
        this.tipoEvento = tipoEvento; // Assegna il tipo di evento
        this.squadra = squadra; // Assegna la squadra
        this.durataPallinaSecondi = durataPallinaSecondi; // Assegna la durata in secondi
    }

    /**
     * Costruttore predefinito senza argomenti.
     * Necessario per framework di persistenza o librerie di mappatura (come Hibernate o Gson).
     */
    public Calciobalilla() {
        // Costruttore vuoto
    }

    /**
     * Recupera l'ID del dispositivo.
     * @return String identificativa del dispositivo.
     */
    public String getId() {
        return idDispositivo; // Ritorna l'identificativo
    }

    /**
     * Imposta l'ID del dispositivo.
     * @param id Il nuovo identificativo da assegnare.
     */
    public void setId(String id) {
        this.idDispositivo = id; // Aggiorna l'ID del dispositivo
    }

    /**
     * Recupera il timestamp dell'evento.
     * @return Long rappresentante il timestamp.
     */
    public Long getTimestamp() {
        return timestamp; // Ritorna il timestamp
    }

    /**
     * Imposta il timestamp dell'evento.
     * @param timestamp Il nuovo timestamp.
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp; // Aggiorna il timestamp
    }

    /**
     * Recupera il tipo di evento.
     * @return String con il tipo di evento.
     */
    public String getTipoEvento() {
        return tipoEvento; // Ritorna il tipo evento
    }

    /**
     * Imposta il tipo di evento.
     * @param tipoEvento Stringa descrittiva dell'evento.
     */
    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento; // Aggiorna il tipo evento
    }

    /**
     * Recupera la squadra associata.
     * @return String con il nome della squadra.
     */
    public String getSquadra() {
        return squadra; // Ritorna la squadra
    }

    /**
     * Imposta la squadra associata.
     * @param squadra Il nome della squadra.
     */
    public void setSquadra(String squadra) {
        this.squadra = squadra; // Aggiorna la squadra
    }

    /**
     * Recupera la durata della pallina in secondi.
     * @return int rappresentante i secondi.
     */
    public int getDurataPallinaSecondi() {
        return durataPallinaSecondi; // Ritorna la durata in secondi
    }

    /**
     * Imposta la durata della pallina in secondi.
     * @param durataPallinaSecondi Numero di secondi trascorsi.
     */
    public void setDurataPallinaSecondi(int durataPallinaSecondi) {
        this.durataPallinaSecondi = durataPallinaSecondi; // Aggiorna la durata
    }
}
