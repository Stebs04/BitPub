package com.bitpub.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * GameSessionEntity - Rappresenta una sessione di gioco attiva o conclusa su un tavolo.
 * * Refactoring Senior Note:
 * L'aggiunta di @Version è qui fondamentale perché lo stato della sessione (ACTIVE, FINISHED)
 * viene aggiornato sia dai task di timeout che dai segnali MQTT provenienti dall'hardware.
 */
@Entity
@Table(name = "game_sessions")
public class GameSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String venueId;

    private Integer tableId;

    private Long userId;

    private String gameType; // CALCIOBALILLA, BILIARDO, FRECCETTE

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status; // ACTIVE, FINISHED, FORCE_STOPPED

    @Version
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }
    public Integer getTableId() { return tableId; }
    public void setTableId(Integer tableId) { this.tableId = tableId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}