package com.bitpub.domain;

import com.google.gson.annotations.Expose;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Rappresenta una specifica partita di Freccette all'interno del sistema.
 * <p>
 * Questa classe estende {@link Partita}, ereditandone tutte le caratteristiche comuni
 * (ID, orari, torneo) e aggiungendo le statistiche e le configurazioni tipiche
 * di una sfida al bersaglio.
 * </p>
 * @author Timothy Giolito 20054431
 * @author Stefano Bellan 20054330 (integrazione e adattamento)
 */
@Entity
@Table(name = "partita_freccette")
// La parola chiave "extends" significa che PartitaFreccette è "figlia" di Partita.
// Eredita automaticamente orarioInizio, orarioFine, torneo e tipoGioco!
public class PartitaFreccette extends Partita {

    /**
     * Il nome del giocatore che ha vinto la partita.
     */
    @Expose
    private String giocatoreVincitore;

    /**
     * Il punteggio finale o la variante di punteggio raggiunta.
     */
    @Expose
    private int punteggio;

    /**
     * Il numero di mosse (lanci) effettuati nella partita.
     */
    @Expose
    private int mosse;

    /**
     * Indica la variante del gioco utilizzata per la partita.
     * Esempi comuni sono: "501", "301", "Cricket" o "Around the Clock".
     */
    @Expose
    private String modalita;

    /**
     * Conta quante volte i giocatori hanno ottenuto il punteggio massimo di 180
     * punti con un singolo set di tre freccette.
     */
    @Expose
    private int numero180;

    /**
     * Esprime la precisione dei giocatori nel colpire il centro del bersaglio (Bullseye).
     * Il valore è inteso come percentuale (es. 15.5 rappresenta il 15.5%).
     */
    @Expose
    private double percentualeBullseye;

    /**
     * Costruttore predefinito.
     * Inizializza l'entità impostando automaticamente il tipo di gioco su "FRECCETTE",
     * informazione fondamentale per la corretta deserializzazione dei dati JSON.
     */
    public PartitaFreccette() {
        super.setTipoGioco("FRECCETTE");
    }

    /**
     * Costruttore completo per creare una partita di freccette con statistiche iniziali.
     *
     * @param modalita La variante di gioco scelta.
     * @param numero180 Il numero iniziale di punteggi massimi registrati.
     * @param percentualeBullseye La percentuale di precisione sul centro.
     */
    public PartitaFreccette(String modalita, int numero180, double percentualeBullseye) {
        super.setTipoGioco("FRECCETTE");
        this.modalita = modalita;
        this.numero180 = numero180;
        this.percentualeBullseye = percentualeBullseye;
    }

    // --- GETTER E SETTER ---

    public String getGiocatoreVincitore() {
        return giocatoreVincitore;
    }

    public void setGiocatoreVincitore(String giocatoreVincitore) {
        this.giocatoreVincitore = giocatoreVincitore;
    }

    public int getPunteggio() {
        return punteggio;
    }

    public void setPunteggio(int punteggio) {
        this.punteggio = punteggio;
    }

    public int getMosse() {
        return mosse;
    }

    public void setMosse(int mosse) {
        this.mosse = mosse;
    }

    /** @return La modalità di gioco (es. "501"). */
    public String getModalita() {
        return modalita;
    }

    /** @param modalita Imposta la variante di gioco. */
    public void setModalita(String modalita) {
        this.modalita = modalita;
    }

    /** @return Il numero totale di "180" effettuati. */
    public int getNumero180() {
        return numero180;
    }

    /** @param numero180 Imposta il numero di "180". */
    public void setNumero180(int numero180) {
        this.numero180 = numero180;
    }

    /** @return La percentuale di precisione sul centro. */
    public double getPercentualeBullseye() {
        return percentualeBullseye;
    }

    /** @param percentualeBullseye Imposta la percentuale di precisione sul centro. */
    public void setPercentualeBullseye(double percentualeBullseye) {
        this.percentualeBullseye = percentualeBullseye;
    }
}

