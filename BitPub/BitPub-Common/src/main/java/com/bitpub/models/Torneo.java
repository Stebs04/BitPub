/**
 * Entità che rappresenta un Torneo organizzato in un locale.
 * * @author Luca Franzon
 * @version 1.0
 */
package com.bitpub.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tornei")
public class Torneo extends ResourceModel { // Estende ResourceModel per supporto HATEOAS

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoGioco tipoGioco;

    private Long localeId;

    private LocalDateTime dataInizio;

    private Integer maxPartecipanti;

    @Enumerated(EnumType.STRING)
    private ModalitaTorneo modalita;

    // Enum interni per consistenza dati
    public enum TipoGioco { CALCIOBALILLA, FRECCETTE, BILIARDO }
    public enum ModalitaTorneo { INDIVIDUALE, SQUADRE }


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

    public Integer getMaxPartecipanti() { return maxPartecipanti; }
    public void setMaxPartecipanti(Integer maxPartecipanti) { this.maxPartecipanti = maxPartecipanti; }

    public List<Utente> getIscritti() { return iscritti; }
    public void setIscritti(List<Utente> iscritti) { this.iscritti = iscritti; }
}