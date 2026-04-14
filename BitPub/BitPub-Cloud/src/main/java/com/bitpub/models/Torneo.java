package com.bitpub.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
public class Torneo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String premio;

    private LocalDate dataInizio;
    private LocalDate dataFine;

    // Relazione: Un torneo ha una lista di molte partite.
    // "mappedBy" indica il nome della variabile nella classe Partita.
    // "cascade = CascadeType.ALL" significa che se eliminiamo il torneo,
    // eliminiamo automaticamente anche le partite collegate!
    @OneToMany(mappedBy = "torneo", cascade = CascadeType.ALL)
    private List<Partita> partite;

    // Costruttore vuoto
    public Torneo() {}

    // Costruttore con parametri
    public Torneo(String nome, String premio, LocalDate dataInizio, LocalDate dataFine) {
        this.nome = nome;
        this.premio = premio;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
    }

    // --- GETTER E SETTER ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getPremio() { return premio; }
    public void setPremio(String premio) { this.premio = premio; }

    public LocalDate getDataInizio() { return dataInizio; }
    public void setDataInizio(LocalDate dataInizio) { this.dataInizio = dataInizio; }

    public LocalDate getDataFine() { return dataFine; }
    public void setDataFine(LocalDate dataFine) { this.dataFine = dataFine; }

    public List<Partita> getPartite() { return partite; }
    public void setPartite(List<Partita> partite) { this.partite = partite; }
}