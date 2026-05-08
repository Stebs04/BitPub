package com.bitpub.models;

/**
 * Modello che rappresenta le statistiche di gioco di un utente al Calciobalilla.
 */
public class StatisticheCalciobalilla {

    private int vinte;
    private int perse;
    private int golFatti;
    private int golSubiti;

    // Costruttore vuoto (spesso utile per le interfacce grafiche o per creare l'oggetto partendo da zero)
    public StatisticheCalciobalilla() {
        this.vinte = 0;
        this.perse = 0;
        this.golFatti = 0;
        this.golSubiti = 0;
    }

    // Costruttore con i parametri
    public StatisticheCalciobalilla(int vinte, int perse, int golFatti, int golSubiti) {
        this.vinte = vinte;
        this.perse = perse;
        this.golFatti = golFatti;
        this.golSubiti = golSubiti;
    }

    // --- Metodi "Getter" (Quelli che l'interfaccia grafica stava cercando disperatamente!) ---

    public int getVinte() {
        return vinte;
    }

    public int getPerse() {
        return perse;
    }

    public int getGolFatti() {
        return golFatti;
    }

    public int getGolSubiti() {
        return golSubiti;
    }

    // --- Metodi "Setter" (Per aggiornare i dati in futuro) ---

    public void setVinte(int vinte) {
        this.vinte = vinte;
    }

    public void setPerse(int perse) {
        this.perse = perse;
    }

    public void setGolFatti(int golFatti) {
        this.golFatti = golFatti;
    }

    public void setGolSubiti(int golSubiti) {
        this.golSubiti = golSubiti;
    }
}