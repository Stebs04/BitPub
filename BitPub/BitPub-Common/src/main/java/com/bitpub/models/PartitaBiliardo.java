package com.bitpub.models; // Modifica il package in base a dove l'ha messo Timothy

import com.google.gson.annotations.Expose;

/**
 * Modello specifico per le partite di Biliardo.
 * Estende la classe base Partita ereditandone i dati comuni.
 */
public class PartitaBiliardo extends Partita {

    // Campi specifici del Biliardo
    @Expose
    private String specialita;
    @Expose
    private int serieMassimaPalleImbucate;
    @Expose
    private int falli;

    /**
     * Costruttore vuoto.
     * Molto importante per GSON: quando trasforma il JSON in oggetto,
     * usa spesso il costruttore di default.
     */
    public PartitaBiliardo() {
        super();
        // Impostiamo il discriminatore affinché il JSON generato abbia sempre questo valore.
        // Assicurati che Timothy abbia definito 'tipoGioco' come 'protected' o con un setter in 'Partita'.
        this.setTipoGioco("BILIARDO");
    }

    /**
     * Costruttore con parametri per creare facilmente una partita.
     */
    public PartitaBiliardo(String specialita, int serieMassimaPalleImbucate, int falli) {
        super();
        this.setTipoGioco("BILIARDO");
        this.specialita = specialita;
        this.serieMassimaPalleImbucate = serieMassimaPalleImbucate;
        this.falli = falli;
    }

    // --- GETTER E SETTER ---

    public String getSpecialita() {
        return specialita;
    }

    public void setSpecialita(String specialita) {
        this.specialita = specialita;
    }

    public int getSerieMassimaPalleImbucate() {
        return serieMassimaPalleImbucate;
    }

    public void setSerieMassimaPalleImbucate(int serieMassimaPalleImbucate) {
        this.serieMassimaPalleImbucate = serieMassimaPalleImbucate;
    }

    public int getFalli() {
        return falli;
    }

    public void setFalli(int falli) {
        this.falli = falli;
    }

    // Un metodo utile per stampare a schermo i dati durante i test
    @Override
    public String toString() {
        return "PartitaBiliardo{" +
                "specialita='" + specialita + '\'' +
                ", serieMassimaPalleImbucate=" + serieMassimaPalleImbucate +
                ", falli=" + falli +
                "} " + super.toString(); // Richiama anche la stampa della superclasse
    }
}