package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Singolo scontro del tabellone a eliminazione diretta.
 * round 0 = primo turno; matchIndex = posizione nel round (0-based).
 * nextMatchId = match a cui avanza il vincitore (null nella finale).
 */
@Entity
@Table(name = "tournament_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private int matchIndex;

    private String player1Id;
    private String player1Name;
    private String player2Id;
    private String player2Name;

    private String winnerId;
    private String winnerName;

    private String score; // punteggio / statistiche dello scontro (testo libero)

    // Gol per slot, ingeriti via MQTT (bitpub/cloud/matches/result). Fonte isolata dei gol del torneo:
    // solo gli scontri del tabellone contribuiscono, mai la leaderboard globale.
    @Column(name = "player1_goals")
    private int player1Goals;
    @Column(name = "player2_goals")
    private int player2Goals;

    private String nextMatchId;
}
