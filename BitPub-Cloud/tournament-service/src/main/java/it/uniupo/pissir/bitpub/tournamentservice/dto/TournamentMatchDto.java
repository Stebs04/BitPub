/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.dto;

import lombok.*;

/**
 * Data Transfer Object per i singoli match del torneo.
 * Include i dati sui partecipanti, il risultato dello scontro e l'avanzamento nel tabellone.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentMatchDto {
    private String id;
    private int round;
    private int matchIndex;
    private String player1Id;
    private String player1Name;
    private String player2Id;
    private String player2Name;
    private String winnerId;
    private String winnerName;
    private String score;
    private String nextMatchId;
}
