/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object che rappresenta un torneo.
 * Viene utilizzato per scambiare informazioni strutturate sui tornei tra il backend e i client esterni.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentDto {
    private String id;
    private String name;
    private String gameTypeId;
    private boolean teamBased;
    private List<String> localeIds; // Lista degli identificativi dei locali associati a questo torneo
    private Instant startDate;
    private Instant endDate;
    private String status;
    private Integer maxParticipants;
    private List<TournamentRegistrationDto> registrations;
    private List<TournamentMatchDto> bracket;
}
