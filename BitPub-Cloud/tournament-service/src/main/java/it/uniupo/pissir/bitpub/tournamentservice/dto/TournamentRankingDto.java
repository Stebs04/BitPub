/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.dto;

import lombok.*;

/**
 * Data Transfer Object per la classifica di un torneo.
 * Trasporta le statistiche di ogni partecipante (gol, partite vinte) calcolate durante la competizione.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRankingDto {
    private String id;
    private String tournamentId;
    private String participantId;
    private String participantName;
    private int goalsScored;
    private int matchesPlayed;
    private int matchesWon;
    private int currentRank;
}
