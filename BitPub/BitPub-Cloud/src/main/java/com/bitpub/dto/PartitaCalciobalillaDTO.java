package com.bitpub.dto;

import java.time.LocalDateTime;

public class PartitaCalciobalillaDTO {
    private Long id;
    private String nomeSquadraRossa;
    private String nomeSquadraBlu;
    private Integer punteggioRossi;
    private Integer punteggioBlu;
    private Integer squalificheRossi;
    private Integer squalificheBlu;
    private Integer totaleRullate;
    private LocalDateTime dataFine;
    private Long torneoId;

    public PartitaCalciobalillaDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeSquadraRossa() { return nomeSquadraRossa; }
    public void setNomeSquadraRossa(String r) { this.nomeSquadraRossa = r; }
    public String getNomeSquadraBlu() { return nomeSquadraBlu; }
    public void setNomeSquadraBlu(String b) { this.nomeSquadraBlu = b; }
    public Integer getPunteggioRossi() { return punteggioRossi; }
    public void setPunteggioRossi(Integer v) { this.punteggioRossi = v; }
    public Integer getPunteggioBlu() { return punteggioBlu; }
    public void setPunteggioBlu(Integer v) { this.punteggioBlu = v; }
    public Integer getSqualificheRossi() { return squalificheRossi; }
    public void setSqualificheRossi(Integer v) { this.squalificheRossi = v; }
    public Integer getSqualificheBlu() { return squalificheBlu; }
    public void setSqualificheBlu(Integer v) { this.squalificheBlu = v; }
    public Integer getTotaleRullate() { return totaleRullate; }
    public void setTotaleRullate(Integer v) { this.totaleRullate = v; }
    public LocalDateTime getDataFine() { return dataFine; }
    public void setDataFine(LocalDateTime v) { this.dataFine = v; }
    public Long getTorneoId() { return torneoId; }
    public void setTorneoId(Long id) { this.torneoId = id; }
}
