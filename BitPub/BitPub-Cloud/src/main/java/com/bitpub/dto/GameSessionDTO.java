package com.bitpub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * GameSessionDTO - Data Transfer Object per la gestione delle sessioni di gioco.
 *
 * Questa classe definisce lo standard architetturale per tutti i DTO del progetto BitPub.
 * @author Stefano Bellan 20054330
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameSessionDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("locale_id")
    private Long localeId;

    @JsonProperty("tavolo_id")
    private String tavoloId;

    @JsonProperty("utente_id")
    private Long utenteId;

    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @JsonProperty("end_time")
    private LocalDateTime endTime;

    @JsonProperty("status")
    private String status;

    /**
     * Costruttore di default obbligatorio per la corretta deserializzazione 
     * tramite Jackson (es. quando il framework converte il JSON in oggetto Java).
     */
    public GameSessionDTO() {
    }

    /**
     * Costruttore completo per agevolare il mapping manuale o tramite MapStruct/ModelAssembler.
     */
    public GameSessionDTO(Long id, Long localeId, String tavoloId, Long utenteId, 
                          LocalDateTime startTime, LocalDateTime endTime, String status) {
        this.id = id;
        this.localeId = localeId;
        this.tavoloId = tavoloId;
        this.utenteId = utenteId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // ==========================================
    // GETTER & SETTER (Standard Java Bean)
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLocaleId() {
        return localeId;
    }

    public void setLocaleId(Long localeId) {
        this.localeId = localeId;
    }

    public String getTavoloId() {
        return tavoloId;
    }

    public void setTavoloId(String tavoloId) {
        this.tavoloId = tavoloId;
    }

    public Long getUtenteId() {
        return utenteId;
    }

    public void setUtenteId(Long utenteId) {
        this.utenteId = utenteId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}