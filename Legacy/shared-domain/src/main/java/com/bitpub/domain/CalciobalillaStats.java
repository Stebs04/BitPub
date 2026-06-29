package com.bitpub.domain;

import com.google.gson.annotations.Expose;

/**
 * Modello di dominio per le statistiche aggregate del Calciobalilla.
 * Rappresenta i dati sintetici derivanti dall'analisi delle partite giocate,
 * pronto per essere serializzato in formato JSON e inviato ai client.
 *
 * @author Stefano Bellan 20054330
 */
public class CalciobalillaStats {

    /** Conteggio totale dei falli (rullate) rilevati dai sensori del sistema */
    @Expose
    private int totaleRullateSistema;

    /** Numero totale di partite vinte dalla squadra Rossa */
    @Expose
    private int vittorieRossi;

    /** Numero totale di partite vinte dalla squadra Blu */
    @Expose
    private int vittorieBlu;

    /**
     * Costruttore completo per l'inizializzazione delle statistiche.
     *
     * @param rullate  Il numero totale di rullate rilevate.
     * @param vRossi   Il totale vittorie della squadra Rossa.
     * @param vBlu     Il totale vittorie della squadra Blu.
     */
    public CalciobalillaStats(int rullate, int vRossi, int vBlu) {
        this.totaleRullateSistema = rullate;
        this.vittorieRossi = vRossi;
        this.vittorieBlu = vBlu;
    }

    // --- GETTER (Necessari per la serializzazione Jackson/GSON) ---

    /** @return Il conteggio totale delle rullate registrate. */
    public int getTotaleRullateSistema() {
        return totaleRullateSistema;
    }

    /** @return Il numero complessivo di vittorie dei Rossi. */
    public int getVittorieRossi() {
        return vittorieRossi;
    }

    /** @return Il numero complessivo di vittorie dei Blu. */
    public int getVittorieBlu() {
        return vittorieBlu;
    }
}


