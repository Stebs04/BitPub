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
}
