// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per la richiesta da parte di un giocatore di accedere alla lobby di una specifica istanza di gioco.
 * Il nome utente (username) è contenuto nel corpo della richiesta in quanto il gateway non supporta 
 * nativamente l'inoltro di un claim dedicato; l'identificativo del giocatore, invece, viene estratto 
 * in modo sicuro dall'header X-User-Id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinLobbyRequestDto {
    private String gameInstanceId;
    private String username;
    // Quando valorizzato, identifica una partita di torneo: l'accesso è riservato esclusivamente 
    // ai due giocatori previsti dal tabellone per questo scontro. Un valore nullo indica una partita libera.
    private String tournamentMatchId;
}
