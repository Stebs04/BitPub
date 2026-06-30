package it.uniupo.pissir.bitpub.tournamentservice.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentDto {
    private String id;
    private String name;
    private String gameTypeId;
    private boolean teamBased;
    private Instant startDate;
    private Instant endDate;
    private String status;
    private List<TournamentRegistrationDto> registrations;
}
