package com.bitpub.tournament.mapper;

import com.bitpub.tournament.dto.*;
import com.bitpub.tournament.model.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TournamentMapper {

    public TournamentDto toDto(Tournament tournament) {
        if (tournament == null) return null;
        return TournamentDto.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .gameId(tournament.getGameId())
                .format(tournament.getFormat())
                .status(tournament.getStatus())
                .maxParticipants(tournament.getMaxParticipants())
                .teamSize(tournament.getTeamSize())
                .locationIds(tournament.getLocationIds())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .build();
    }

    public TeamDto toDto(Team team) {
        if (team == null) return null;
        return TeamDto.builder()
                .id(team.getId())
                .tournamentId(team.getTournament() != null ? team.getTournament().getId() : null)
                .name(team.getName())
                .seed(team.getSeed())
                .status(team.getStatus())
                .players(team.getPlayers().stream().map(p -> 
                        RegisterTeamRequest.PlayerDto.builder()
                                .userId(p.getUserId())
                                .username(p.getUsername())
                                .build()
                ).collect(Collectors.toList()))
                .build();
    }

    public TournamentMatchDto toDto(TournamentMatch match) {
        if (match == null) return null;
        return TournamentMatchDto.builder()
                .id(match.getId())
                .tournamentId(match.getTournament() != null ? match.getTournament().getId() : null)
                .round(match.getRound())
                .matchIndex(match.getMatchIndex())
                .teamA(toDto(match.getTeamA()))
                .teamB(toDto(match.getTeamB()))
                .winner(toDto(match.getWinner()))
                .scoreA(match.getScoreA())
                .scoreB(match.getScoreB())
                .nextMatchId(match.getNextMatchId())
                .status(match.getStatus())
                .build();
    }

    public LeaderboardEntryDto toDto(LeaderboardEntry entry) {
        if (entry == null) return null;
        return LeaderboardEntryDto.builder()
                .id(entry.getId())
                .tournamentId(entry.getTournament() != null ? entry.getTournament().getId() : null)
                .team(toDto(entry.getTeam()))
                .points(entry.getPoints())
                .wins(entry.getWins())
                .losses(entry.getLosses())
                .draws(entry.getDraws())
                .goalsFor(entry.getGoalsFor())
                .goalsAgainst(entry.getGoalsAgainst())
                .build();
    }
}
