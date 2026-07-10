/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entità che modella una singola partita all'interno del tabellone a eliminazione diretta.
 * Contiene informazioni sul turno di gioco, la posizione dello scontro e il percorso di avanzamento
 * (il vincitore prosegue nel match indicato da nextMatchId).
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

    private String score; // Testo libero che descrive il punteggio o i dettagli finali dell'incontro

    // Gol segnati dai due partecipanti durante la partita.
    // Questi dati vengono ricevuti via MQTT e contribuiscono esclusivamente alle statistiche interne al torneo, senza influenzare la classifica globale.
    @Column(name = "player1_goals")
    private int player1Goals;
    @Column(name = "player2_goals")
    private int player2Goals;

    private String nextMatchId;
}
