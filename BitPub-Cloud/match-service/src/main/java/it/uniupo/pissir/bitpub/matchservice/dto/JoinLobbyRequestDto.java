package it.uniupo.pissir.bitpub.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Richiesta del PLAYER per entrare in lobby su una gameInstance specifica.
 * Lo username viaggia nel body (e non come header) perche' il gateway non
 * inoltra ancora un claim X-Username; playerId arriva invece da X-User-Id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinLobbyRequestDto {
    private String gameInstanceId;
    private String username;
    // Se valorizzato, la lobby e' una partita di torneo: solo i due giocatori abbinati
    // in questo scontro del tabellone possono connettersi. Null = partita libera.
    private String tournamentMatchId;
}
