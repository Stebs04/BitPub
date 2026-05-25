package com.bitpub.tournament.dto;

import com.bitpub.tournament.model.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentMatchDto {
    private UUID id;
    private UUID tournamentId;
    private int round;
    private int matchIndex;
    private TeamDto teamA;
    private TeamDto teamB;
    private TeamDto winner;
    private int scoreA;
    private int scoreB;
    private UUID nextMatchId;
    private MatchStatus status;
}
