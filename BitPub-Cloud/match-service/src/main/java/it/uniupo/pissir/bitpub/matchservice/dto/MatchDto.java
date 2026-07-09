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
    /** true = partita a squadre (registra il Team), false = individuale. */
    private boolean teamBased;
    private Instant startTime;
    private Instant endTime;
    // Slot/partecipanti della partita. Field JSON resta "teams" per non rompere edge + WebApp.
    private List<ParticipantResponseDto> teams;
    private String resultPayload;
    /** ID utente del vincitore (null se pareggio o partita non terminata). */
    private String winnerId;

    // Stato del gameplay a turni (esposto al frontend per abilitare/disabilitare i controlli).
    private String currentTurnUserId;
    private boolean breakDone;
    private String solidTeamId;
    private String stripedTeamId;
}
