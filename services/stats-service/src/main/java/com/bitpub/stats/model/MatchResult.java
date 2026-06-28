package com.bitpub.stats.model;

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
@Table(name = "match_results", schema = "stats")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "match_session_id", nullable = false)
    private UUID matchSessionId;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "winner_user_id")
    private UUID winnerUserId;

    @Column(name = "loser_user_id")
    private UUID loserUserId;

    @Column(name = "winner_score")
    private int winnerScore;

    @Column(name = "loser_score")
    private int loserScore;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    @PrePersist
    protected void onCreate() {
        playedAt = LocalDateTime.now();
    }
}
