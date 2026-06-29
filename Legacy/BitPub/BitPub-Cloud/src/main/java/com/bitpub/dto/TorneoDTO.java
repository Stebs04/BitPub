package com.bitpub.dto;

import java.time.LocalDate;

public class TorneoDTO {
    private Long id;
    private String nome;
    private String tipoGioco;
    private Long localeId;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private String premio;
    private Integer maxPartecipanti;
    private String modalita;

    public TorneoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTipoGioco() { return tipoGioco; }
    public void setTipoGioco(String tipoGioco) { this.tipoGioco = tipoGioco; }
    public Long getLocaleId() { return localeId; }
    public void setLocaleId(Long localeId) { this.localeId = localeId; }
    public LocalDate getDataInizio() { return dataInizio; }
    public void setDataInizio(LocalDate dataInizio) { this.dataInizio = dataInizio; }
    public LocalDate getDataFine() { return dataFine; }
    public void setDataFine(LocalDate dataFine) { this.dataFine = dataFine; }
    public String getPremio() { return premio; }
    public void setPremio(String premio) { this.premio = premio; }
    public Integer getMaxPartecipanti() { return maxPartecipanti; }
    public void setMaxPartecipanti(Integer maxPartecipanti) { this.maxPartecipanti = maxPartecipanti; }
    public String getModalita() { return modalita; }
    public void setModalita(String modalita) { this.modalita = modalita; }
}