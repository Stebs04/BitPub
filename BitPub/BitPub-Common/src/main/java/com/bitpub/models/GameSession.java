package com.bitpub.repository;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * GameSessionEntity - Rappresenta una sessione di gioco attiva o conclusa su un tavolo.
 * * Refactoring Senior Note:
 * L'aggiunta di @Version è qui fondamentale perché lo stato della sessione (ACTIVE, FINISHED)
 * viene aggiornato sia dai task di timeout che dai segnali MQTT provenienti dall'hardware.
 */
@Entity
@Table(name = "game_sessions")
@Data
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
}