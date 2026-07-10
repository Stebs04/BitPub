/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Rappresenta la posizione in classifica di un partecipante all'interno di un torneo specifico.
 * Raccoglie i dati aggregati relativi alle performance maturate unicamente durante la competizione.
 */
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
    private int goalsScored; // Somma dei gol realizzati esclusivamente nelle partite ufficiali del torneo corrente
    
    @Column(nullable = false)
    private int currentRank; // Posizione occupata attualmente dal partecipante nella classifica generale del torneo
}
