package it.uniupo.pissir.bitpub.tournamentservice.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRegistrationDto {
    private String id;
    private String tournamentId;
    private String participantId;
    private String participantName;
    private boolean team;          // true = iscrizione a squadre
    private List<String> members;  // membri della squadra (o singolo giocatore)
    private String localeId;
    private Instant registeredAt;

    // Statistiche del partecipante in QUESTO torneo, derivate dal tabellone (gol via MQTT).
    private int matchesPlayed;    // scontri conclusi giocati nel torneo
    private int goalsScored;      // gol segnati nelle partite del torneo
    private String currentStage;  // fase raggiunta: Finale, Semifinale, ...
    private String tournamentStatus; // UPCOMING | ACTIVE | COMPLETED — per nascondere i tornei conclusi
}
