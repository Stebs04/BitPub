// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchDto {
    private String id;
    private String gameInstanceId;
    private String localeId;
    private String gameTypeId;
    private String status;
    /** Flag booleano: true per partite a squadre (dove si registrano i team), false per le individuali. */
    private boolean teamBased;
    private Instant startTime;
    private Instant endTime;
    // Rappresenta le fazioni o partecipanti della partita. Il campo viene serializzato come "teams" 
    // per preservare la retrocompatibilità con i componenti Edge e WebApp.
    private List<ParticipantResponseDto> teams;
    private String resultPayload;
    /** Identificativo utente del vincitore (nullo nel caso di pareggio o partita non terminata). */
    private String winnerId;

    // Stato di avanzamento dei turni (fornito al client per l'abilitazione dei relativi controlli interfaccia).
    private String currentTurnUserId;
    private boolean breakDone;
    private String solidTeamId;
    private String stripedTeamId;
}
