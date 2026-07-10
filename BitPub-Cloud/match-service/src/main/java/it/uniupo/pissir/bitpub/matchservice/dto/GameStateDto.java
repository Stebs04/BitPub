// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateDto {
    private String matchId;
    private String gameTypeId;
    private String status; // Stato corrente della partita (es. WAITING, PLAYING, FINISHED)
    private String teamAName;  // Nome visualizzato per la squadra o giocatore A
    private String teamBName;  // Nome visualizzato per la squadra o giocatore B
    private int scoreTeamA;
    private int scoreTeamB;
    private int timeRemainingSeconds;
    private String currentEventMessage; // Messaggio di evento in corso (es. "GOAL!", "MATCH STARTED")
    private String winnerName;          // Popolato al termine della partita (MATCH_END)
    private String currentTurnUserId;   // Inizializzato dal Cloud all'avvio della partita e mantenuto aggiornato dall'Edge
}
