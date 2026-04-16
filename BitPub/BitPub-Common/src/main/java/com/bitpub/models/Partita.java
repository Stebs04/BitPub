package com.bitpub.models;

import com.google.gson.annotations.Expose;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "partite")
// Questa annotazione dice a Spring/Hibernate: "Crea una tabella per i campi comuni qui,
// e tabelle separate per i campi specifici di Biliardo o Freccette, unendole con un JOIN"
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Partita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Expose private Long id;

    // Usiamo LocalDateTime per registrare giorno e ora esatta dell'evento
    @Expose private LocalDateTime orarioInizio;
    @Expose private LocalDateTime orarioFine;

    @Expose private String tipoGioco;

    // Relazione: Molte partite possono appartenere a un solo Torneo
    @ManyToOne
    @JoinColumn(name = "torneo_id") // Crea la colonna "chiave esterna" nel database
    private Torneo torneo;

    // Costruttore vuoto (obbligatorio per JPA e GSON)
    public Partita() {}

    // Costruttore con parametri
    public Partita(LocalDateTime orarioInizio, String tipoGioco) {
        this.orarioInizio = orarioInizio;
        this.tipoGioco = tipoGioco;
    }

    // --- GETTER E SETTER ---
    // (Necessari per permettere a Spring e GSON di leggere/scrivere le variabili)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getOrarioInizio() { return orarioInizio; }
    public void setOrarioInizio(LocalDateTime orarioInizio) { this.orarioInizio = orarioInizio; }

    public LocalDateTime getOrarioFine() { return orarioFine; }
    public void setOrarioFine(LocalDateTime orarioFine) { this.orarioFine = orarioFine; }

    public String getTipoGioco() { return tipoGioco; }
    public void setTipoGioco(String tipoGioco) { this.tipoGioco = tipoGioco; }

    public Torneo getTorneo() { return torneo; }
    public void setTorneo(Torneo torneo) { this.torneo = torneo; }
}