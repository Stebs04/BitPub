package com.bitpub.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entità JPA che mappa la tabella "game_session" nel database PostgreSQL.
 * Gestita dal backend Spring Boot per la persistenza delle sessioni.
 */
@Entity
@Table(name = "game_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Specifica il tipo di gioco: "FOOSBALL", "DARTS", "BILLIARDS"
    @Column(name = "game_type", nullable = false)
    private String gameType;

    // L'ID del tavolo fisico/simulato (es. 1, 2, 3...)
    @Column(name = "table_id", nullable = false)
    private Integer tableId;

    // Foreign Key (logica) che punta all'utente che ha avviato la partita
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Stato attuale della sessione: "IN_PROGRESS", "FINISHED", "FORCE_STOPPED"
    @Column(name = "status", nullable = false)
    private String status;

    // Punteggi attuali (usati principalmente per il Calciobalilla)
    @Column(name = "score_blue", nullable = false)
    private Integer scoreBlue = 0;

    @Column(name = "score_red", nullable = false)
    private Integer scoreRed = 0;

    // Timestamp di inizio partita
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    // Timestamp di fine partita (nullable, poiché in corso non ha fine)
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // Il credito scalato dal portafoglio dell'utente all'avvio
    @Column(name = "cost_deducted", precision = 10, scale = 2)
    private BigDecimal costDeducted;

    /**
     * Callback JPA invocato automaticamente prima dell'inserimento nel database (INSERT).
     * Garantisce che, se non specificato altrimenti, il timestamp di avvio sia valorizzato
     * con l'ora esatta in cui il record viene creato.
     */
    @PrePersist
    protected void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }
}
