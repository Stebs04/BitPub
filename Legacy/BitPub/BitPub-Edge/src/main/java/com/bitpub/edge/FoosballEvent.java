package com.bitpub.edge;

import java.time.LocalDateTime;

/**
 * Value Object architettato per incapsulare e trasportare le metriche e gli eventi 
 * generati dai tavoli da calciobalilla. 
 * Il design della classe impone una rigorosa immutabilità: l'assenza di metodi mutatori 
 * e l'uso del modificatore final per tutte le variabili di istanza garantiscono una 
 * thread-safety nativa. Questa scelta strutturale risulta fondamentale in un contesto 
 * multi-thread come l'Edge Node, in cui i dati vengono generati dai listener hardware 
 * (produttori) e contemporaneamente letti e serializzati dai worker MQTT (consumatori) 
 * attraverso il buffer Store-and-Forward.
 *
 * @author Stefano Bellan 20054330
 */
public final class FoosballEvent {

    // Dichiarazione monolitica delle proprietà interne bloccate per precludere alterazioni accidentali
    private final int    tableId;
    private final Long   sessionId;
    private final String eventType;          // GOAL | FALLO | START | FORCE_STOPPED
    private final int    scoreBlue;
    private final int    scoreRed;
    private final String status;
    private final String winner;
    private final int    totaleRullate;
    private final int    durataMediaPallinaSecondi;
    private final String timestamp;

    /**
     * Costruttore completo per l'aggregazione atomica dei dati dell'evento.
     * Consente la creazione di un'istantanea congelata della partita in un preciso istante.
     * Genera e inietta internamente la marca temporale (timestamp) per impedire 
     * manomissioni cronologiche durante le latenze di rete.
     *
     * @param tableId L'indirizzo fisico o logico della macchina
     * @param sessionId Il puntatore di riferimento alla partita erogato dal Cloud
     * @param eventType La classificazione del trigger generatore (es. GOAL, START)
     * @param scoreBlue Il punteggio parziale o totale della squadra blu
     * @param scoreRed Il punteggio parziale o totale della squadra rossa
     * @param status Il ciclo di vita attuale della sessione
     * @param winner La determinazione della fazione vincente
     * @param totaleRullate Contatore analitico dei comportamenti irregolari (rullate)
     * @param durataMediaPallinaSecondi Metrica di performance sulla durata media degli scambi
     */
    public FoosballEvent(int tableId, Long sessionId, String eventType,
                         int scoreBlue, int scoreRed, String status, String winner,
                         int totaleRullate, int durataMediaPallinaSecondi) {
        this.tableId                    = tableId;
        this.sessionId                  = sessionId;
        this.eventType                  = eventType;
        this.scoreBlue                  = scoreBlue;
        this.scoreRed                   = scoreRed;
        this.status                     = status;
        this.winner                     = winner;
        this.totaleRullate              = totaleRullate;
        this.durataMediaPallinaSecondi  = durataMediaPallinaSecondi;
        
        // Fissaggio crittografico del timestamp ISO-8601 sfruttando l'orologio di sistema locale
        this.timestamp                  = LocalDateTime.now().toString();
    }

    // Strato di incapsulamento unidirezionale: metodi esposti esclusivamente in lettura

    public int    getTableId()                     { return tableId; }
    public Long   getSessionId()                   { return sessionId; }
    public String getEventType()                   { return eventType; }
    public int    getScoreBlue()                   { return scoreBlue; }
    public int    getScoreRed()                    { return scoreRed; }
    public String getStatus()                      { return status; }
    public String getWinner()                      { return winner; }
    public int    getTotaleRullate()               { return totaleRullate; }
    public int    getDurataMediaPallinaSecondi()   { return durataMediaPallinaSecondi; }
    public String getTimestamp()                   { return timestamp; }
}