/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

/** Struttura dati per la ricezione asincrona degli esiti di partita propagati dal match-service al momento della conclusione. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultEvent {
    private String gameTypeId;
    private String winnerName;
    private String loserName;
    private String winnerId;   // Identificativo dell'utente vincente (null in caso di parità)
    private String loserId;    // Identificativo dell'utente sconfitto
    private int winnerScore;
    private int loserScore;
    private String matchId;
    private String tournamentMatchId; // Riferimento allo scontro nel tabellone, valorizzato esclusivamente per match legati a tornei
    private String localeId;
    private boolean teamBased; // Indica se l'evento è riferito a una sfida individuale oppure tra squadre
}
