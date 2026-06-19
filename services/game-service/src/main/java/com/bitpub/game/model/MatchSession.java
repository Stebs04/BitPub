package com.bitpub.game.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "match_sessions")
public class MatchSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "session_id", unique = true)
    private String sessionId;

    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(nullable = false)
    private String status; // ONGOING, COMPLETED, FINISHED, ABORTED
    
    @Column(name = "score_1")
    private Integer score1 = 0;
    
    @Column(name = "score_2")
    private Integer score2 = 0;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }
}
