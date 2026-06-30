package it.uniupo.pissir.bitpub.tournamentservice.dto;

import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRegistrationDto {
    private String id;
    private String tournamentId;
    private String participantId;
    private String participantName;
    private String localeId;
    private Instant registeredAt;
}
