package com.bitpub.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "partita_freccette")
// La parola chiave "extends" significa che PartitaFreccette è "figlia" di Partita.
// Eredita automaticamente orarioInizio, orarioFine, torneo e tipoGioco!
public class PartitaFreccette extends Partita {

    // Identifica se è la modalità classica 501, 301, Cricket, ecc.
    private String modalita;

    // Quante volte i giocatori hanno totalizzato il punteggio massimo in un turno
    private int numero180;

    // Statistica della precisione sul centro del bersaglio (es. 15.5 per 15.5%)
    private double percentualeBullseye;

    // Costruttore vuoto (sempre obbligatorio per JPA e GSON)
    public PartitaFreccette() {
        // Impostiamo di default il tipo gioco per la magia di Luca con GSON!
        super.setTipoGioco("FRECCETTE");
    }

    // Costruttore con parametri
    public PartitaFreccette(String modalita, int numero180, double percentualeBullseye) {
        super.setTipoGioco("FRECCETTE");
        this.modalita = modalita;
        this.numero180 = numero180;
        this.percentualeBullseye = percentualeBullseye;
    }

    // --- GETTER E SETTER ---
    // Questi servono per leggere e scrivere i valori specifici delle freccette

    public String getModalita() {
        return modalita;
    }

    public void setModalita(String modalita) {
        this.modalita = modalita;
    }

    public int getNumero180() {
        return numero180;
    }

    public void setNumero180(int numero180) {
        this.numero180 = numero180;
    }

    public double getPercentualeBullseye() {
        return percentualeBullseye;
    }

    public void setPercentualeBullseye(double percentualeBullseye) {
        this.percentualeBullseye = percentualeBullseye;
    }
}