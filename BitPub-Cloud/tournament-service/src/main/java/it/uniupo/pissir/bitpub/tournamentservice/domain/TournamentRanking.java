package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tournament_rankings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private String participantId;

    @Column(nullable = false)
    private String participantName;

    private int score;
    private int matchesPlayed;
    private int matchesWon;
    
    @Column(nullable = false)
    private int currentRank; // Posizione in classifica
}
