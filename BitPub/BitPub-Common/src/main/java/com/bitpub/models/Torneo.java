package com.bitpub.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Entità che rappresenta un Torneo organizzato in un locale.
 * @author Luca Franzon
 * @version 1.0
 */
@Entity
@Table(name = "tornei")
public class Torneo extends ResourceModel { // Estende ResourceModel per supporto HATEOAS[cite: 8]

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Impostiamo nullable = false per impedire al DB di salvare valori nulli[cite: 8]
    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gioco", nullable = false)
    private TipoGioco tipoGioco;

    @Column(name = "locale_id", nullable = false)
    private Long localeId;

    // Specifichiamo il nome esatto della colonna nel DB per evitare incomprensioni[cite: 8]
    @Column(name = "data_inizio")
    private LocalDate dataInizio;

    @Column(name = "data_fine")
    private LocalDate dataFine;

    private String premio;

    @Column(name = "max_partecipanti")
    private Integer maxPartecipanti;

    @Enumerated(EnumType.STRING)
    private ModalitaTorneo modalita;

    // Usiamo @Transient perché non vogliamo creare le tabelle di join nel database per ora[cite: 8]
    @Transient
    private List<Partita> partite;

    @Transient
    private List<Utente> iscritti;

    // Enum interni per consistenza dati[cite: 8]
    public enum TipoGioco { CALCIOBALILLA, FRECCETTE, BILIARDO }
    public enum ModalitaTorneo { INDIVIDUALE, SQUADRE }

    // Costruttore vuoto (assolutamente richiesto da JPA e Gson)[cite: 8]
    public Torneo() {}

    // Costruttore con parametri aggiornato[cite: 8]
    public Torneo(String nome, TipoGioco tipoGioco, Long localeId, LocalDate dataInizio, Integer maxPartecipanti, ModalitaTorneo modalita) {
        this.nome = nome;
        this.tipoGioco = tipoGioco;
        this.localeId = localeId;
        this.dataInizio = dataInizio;
        this.maxPartecipanti = maxPartecipanti;
        this.modalita = modalita;
    }

    // --- GETTER E SETTER COMPLETI ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public TipoGioco getTipoGioco() { return tipoGioco; }
    public void setTipoGioco(TipoGioco tipoGioco) { this.tipoGioco = tipoGioco; }

    public Long getLocaleId() { return localeId; }
    public void setLocaleId(Long localeId) { this.localeId = localeId; }

    public String getPremio() { return premio; }
    public void setPremio(String premio) { this.premio = premio; }

    public LocalDate getDataInizio() { return dataInizio; }
    public void setDataInizio(LocalDate dataInizio) { this.dataInizio = dataInizio; }

    public LocalDate getDataFine() { return dataFine; }
    public void setDataFine(LocalDate dataFine) { this.dataFine = dataFine; }

    public ModalitaTorneo getModalita() { return modalita; }
    public void setModalita(ModalitaTorneo modalita) { this.modalita = modalita; }

    public List<Partita> getPartite() { return partite; }
    public void setPartite(List<Partita> partite) { this.partite = partite; }

    public Integer getMaxPartecipanti() { return maxPartecipanti; }
    public void setMaxPartecipanti(Integer maxPartecipanti) { this.maxPartecipanti = maxPartecipanti; }

    public List<Utente> getIscritti() { return iscritti; }
    public void setIscritti(List<Utente> iscritti) { this.iscritti = iscritti; }
}