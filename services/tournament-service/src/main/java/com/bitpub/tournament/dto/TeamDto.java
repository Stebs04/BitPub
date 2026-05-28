package com.bitpub.tournament.dto;

import com.bitpub.tournament.model.ParticipantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDto {
    private UUID id;
    private UUID tournamentId;
    private String name;
    private List<RegisterTeamRequest.PlayerDto> players;
    private int seed;
    private ParticipantStatus status;
}
