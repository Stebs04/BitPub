package com.bitpub.models;

import com.google.gson.annotations.Expose;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entità JPA per la gestione dei dati storici delle partite di Calciobalilla.
 * Mappa le metriche aggregate dai sensori IOT sulla tabella dedicata.
 * @author Stefano Bellan 20054330
 */
@Entity
@Table(name = "partita_calciobalilla")
public class PartitaCalciobalilla extends Partita {

    @Expose
    private int totaleGol;

    @Expose
    private int totaleRullate;

    @Expose
    private int durataMediaPallinaSecondi;

    /**
     * Costruttore no-args obbligatorio per le specifiche JPA.
     * Imposta il discriminatore di tipo gioco di default.
     */
    public PartitaCalciobalilla() {
        super();
        super.setTipoGioco("CALCIOBALILLA");
    }

    /**
     * Costruttore parametrizzato per l'istanziazione da controller o servizi.
     *
     * @param totaleGol Numero complessivo di reti segnate nella sessione.
     * @param totaleRullate Conteggio totale delle infrazioni/rullate rilevate.
     * @param durataMediaPallinaSecondi Tempo medio di permanenza della pallina in campo.
     */
    public PartitaCalciobalilla(int totaleGol, int totaleRullate, int durataMediaPallinaSecondi) {
        super();
        super.setTipoGioco("CALCIOBALILLA");
        this.totaleGol = totaleGol;
        this.totaleRullate = totaleRullate;
        this.durataMediaPallinaSecondi = durataMediaPallinaSecondi;
    }

    // --- Getter e Setter ---

    public int getTotaleGol() { return totaleGol; }

    /** @param totaleGol Il nuovo valore per il conteggio dei gol. */
    public void setTotaleGol(int totaleGol) { this.totaleGol = totaleGol; }

    public int getTotaleRullate() { return totaleRullate; }

    /** @param totaleRullate Il numero di rullate da registrare. */
    public void setTotaleRullate(int totaleRullate) { this.totaleRullate = totaleRullate; }

    public int getDurataMediaPallinaSecondi() { return durataMediaPallinaSecondi; }

    /** @param durataMediaPallinaSecondi Media calcolata in secondi. */
    public void setDurataMediaPallinaSecondi(int durataMediaPallinaSecondi) { this.durataMediaPallinaSecondi = durataMediaPallinaSecondi; }
}
