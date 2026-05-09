package com.bitpub;

/**
 * Modello puro dei dati per un singolo lancio di freccette.
 * Questa classe definisce solo le informazioni, senza logica di rete.
 */
public class EventoFreccette {
    public String tipoEvento;
    public int puntiLancio;
    public int punteggioRimasto;
    public long timestamp;

    public EventoFreccette(String tipoEvento, int puntiLancio, int punteggioRimasto) {
        this.tipoEvento = tipoEvento;
        this.puntiLancio = puntiLancio;
        this.punteggioRimasto = punteggioRimasto;
        this.timestamp = System.currentTimeMillis();
    }
}