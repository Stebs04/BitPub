package com.bitpub.tournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tournament_matches")
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    private int round; // 1 = quarti, 2 = semi, 3 = finale...

    @Column(name = "match_index")
    private int matchIndex; // posizione nel round

    @Column(name = "player_a_id")
    private UUID playerAId;

    @Column(name = "player_b_id")
    private UUID playerBId;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "score_a")
    private int scoreA;

    @Column(name = "score_b")
    private int scoreB;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MatchStatus status = MatchStatus.SCHEDULED;
}
