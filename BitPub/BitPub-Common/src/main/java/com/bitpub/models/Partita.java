package com.bitpub.models;

import com.google.gson.annotations.Expose;
import java.time.LocalDateTime;
import jakarta.persistence.*;

/**
 * Classe astratta che rappresenta l'entità di base per una "Partita" nel sistema BitPub.
 * <p>
 * Questa classe utilizza la strategia di ereditarietà {@code JOINED}, il che significa che
 * nel database avremo una tabella comune per i dati generali (id, orari, tipo) e tabelle
 * separate per le specifiche dei vari giochi che estenderanno questa classe.
 * </p>
 * * @author Timothy Giolito 20054431
 * * @author Stefano Bellan 20054330 (integrazione e adattamento)
 */
@Entity
@Table(name = "partite")

@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Partita {

    /**
     * Identificativo univoco della partita generato automaticamente dal database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Expose private Long id;

    /**
     * Data e ora esatta dell'inizio e della fine della partita.
     */
    @Expose private LocalDateTime orarioInizio;
    @Expose private LocalDateTime orarioFine;

    /**
     * Stringa descrittiva che indica la tipologia di gioco (es. "Biliardo", "Freccette").
     */
    @Expose private String tipoGioco;


    /**
     * Il Torneo all'interno del quale si svolge questa specifica partita.
     * <p>
     * Rappresenta una relazione molti-a-uno: molte partite possono far parte di un solo torneo.
     * </p>
     */
    @ManyToOne
    @JoinColumn(name = "torneo_id")
    private Torneo torneo;

    /**
     * Costruttore predefinito senza parametri.
     * Necessario per il corretto funzionamento dei framework JPA (Hibernate) e GSON.
     */
    public Partita() {}


    /**
     * Costruttore per inizializzare una nuova partita con i dati essenziali.
     *
     * @param orarioInizio L'orario in cui la partita viene avviata.
     * @param tipoGioco Il nome del gioco associato a questa partita.
     */
    public Partita(LocalDateTime orarioInizio, String tipoGioco) {
        this.orarioInizio = orarioInizio;
        this.tipoGioco = tipoGioco;
    }

    // --- GETTER E SETTER ---

    /** @return L'ID univoco della partita. */
    public Long getId() { return id; }

    /** @param id Imposta l'ID univoco della partita. */
    public void setId(Long id) { this.id = id; }

    /** @return L'orario di inizio della partita. */
    public LocalDateTime getOrarioInizio() { return orarioInizio; }

    /** @param orarioInizio Imposta l'orario di inizio. */
    public void setOrarioInizio(LocalDateTime orarioInizio) { this.orarioInizio = orarioInizio; }

    /** @return L'orario di fine della partita (può essere null se non ancora terminata). */
    public LocalDateTime getOrarioFine() { return orarioFine; }

    /** @param orarioFine Imposta l'orario di fine. */
    public void setOrarioFine(LocalDateTime orarioFine) { this.orarioFine = orarioFine; }

    /** @return Il tipo di gioco della partita. */
    public String getTipoGioco() { return tipoGioco; }

    /** @param tipoGioco Imposta il tipo di gioco. */
    public void setTipoGioco(String tipoGioco) { this.tipoGioco = tipoGioco; }

    /** @return Il torneo associato a questa partita. */
    public Torneo getTorneo() { return torneo; }

    /** @param torneo Imposta il torneo di appartenenza. */
    public void setTorneo(Torneo torneo) { this.torneo = torneo; }
}