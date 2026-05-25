package com.bitpub.tournament.service;

import com.bitpub.tournament.dto.CreateTournamentRequest;
import com.bitpub.tournament.dto.RegisterTeamRequest;
import com.bitpub.tournament.dto.SubmitResultRequest;
import com.bitpub.tournament.model.*;
import com.bitpub.tournament.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final TournamentPlayerRepository playerRepository;
    private final TournamentMatchRepository matchRepository;
    private final LeaderboardEntryRepository leaderboardRepository;

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }

    public Tournament getTournamentById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found: " + id));
    }

    @Transactional
    public Tournament createTournament(CreateTournamentRequest request) {
        Tournament tournament = Tournament.builder()
                .name(request.getName())
                .gameId(UUID.fromString(request.getGameId()))
                .format(request.getFormat() != null ? request.getFormat() : TournamentFormat.SINGLE_ELIMINATION)
                .status("REGISTRATION")
                .maxParticipants(request.getMaxParticipants() > 0 ? request.getMaxParticipants() : 16)
                .teamSize(request.getTeamSize() > 0 ? request.getTeamSize() : 1)
                .locationIds(request.getLocationIds() != null ? request.getLocationIds() : new ArrayList<>())
                .startDate(request.getStartDate())
                .build();
        return tournamentRepository.save(tournament);
    }

    @Transactional
    public Team registerTeam(UUID tournamentId, RegisterTeamRequest request) {
        Tournament tournament = getTournamentById(tournamentId);
        if (!"REGISTRATION".equals(tournament.getStatus())) {
            throw new IllegalStateException("Tournament is not open for registration");
        }

        long currentCount = teamRepository.findByTournamentId(tournamentId).size();
        if (currentCount >= tournament.getMaxParticipants()) {
            throw new IllegalStateException("Tournament is full");
        }

        Team team = Team.builder()
                .tournament(tournament)
                .name(request.getName())
                .seed((int) currentCount + 1)
                .build();
        team = teamRepository.save(team);

        for (RegisterTeamRequest.PlayerDto playerDto : request.getPlayers()) {
            TournamentPlayer player = TournamentPlayer.builder()
                    .team(team)
                    .userId(playerDto.getUserId())
                    .username(playerDto.getUsername())
                    .build();
            playerRepository.save(player);
        }

        if (tournament.getFormat() == TournamentFormat.ROUND_ROBIN) {
            LeaderboardEntry entry = LeaderboardEntry.builder()
                    .tournament(tournament)
                    .team(team)
                    .build();
            leaderboardRepository.save(entry);
        }

        return team;
    }

    @Transactional
    public List<TournamentMatch> generateBracket(UUID tournamentId) {
        Tournament tournament = getTournamentById(tournamentId);
        List<Team> teams = teamRepository.findByTournamentId(tournamentId);

        if (teams.size() < 2) {
            throw new IllegalStateException("At least 2 teams required to generate matches");
        }

        tournament.setStatus("ONGOING");
        tournamentRepository.save(tournament);

        if (tournament.getFormat() == TournamentFormat.SINGLE_ELIMINATION) {
            return generateSingleElimination(tournament, teams);
        } else {
            return generateRoundRobin(tournament, teams);
        }
    }

    private List<TournamentMatch> generateSingleElimination(Tournament tournament, List<Team> teams) {
        Collections.shuffle(teams);

        int slots = nextPowerOfTwo(teams.size());
        List<TournamentMatch> matches = new ArrayList<>();

        for (int i = 0; i < slots / 2; i++) {
            Team teamA = (i * 2 < teams.size()) ? teams.get(i * 2) : null;
            Team teamB = (i * 2 + 1 < teams.size()) ? teams.get(i * 2 + 1) : null;

            TournamentMatch match = TournamentMatch.builder()
                    .tournament(tournament)
                    .round(1)
                    .matchIndex(i)
                    .teamA(teamA)
                    .teamB(teamB)
                    .status(teamB == null ? MatchStatus.FINISHED : MatchStatus.SCHEDULED)
                    .winner(teamB == null ? teamA : null)
                    .build();
            matches.add(matchRepository.save(match));
        }

        int totalRounds = (int) (Math.log(slots) / Math.log(2));
        for (int round = 2; round <= totalRounds; round++) {
            int matchesInRound = slots / (int) Math.pow(2, round);
            for (int i = 0; i < matchesInRound; i++) {
                TournamentMatch match = TournamentMatch.builder()
                        .tournament(tournament)
                        .round(round)
                        .matchIndex(i)
                        .status(MatchStatus.SCHEDULED)
                        .build();
                matches.add(matchRepository.save(match));
            }
        }
        
        // Link next_match_id per semplicita di UI (opzionale se gestito dinamicamente, ma comodo)
        for(int r = 1; r < totalRounds; r++) {
            final int currentRound = r;
            List<TournamentMatch> currentRoundMatches = matches.stream().filter(m -> m.getRound() == currentRound).collect(Collectors.toList());
            List<TournamentMatch> nextRoundMatches = matches.stream().filter(m -> m.getRound() == currentRound + 1).collect(Collectors.toList());
            
            for(TournamentMatch cm : currentRoundMatches) {
                int nextIndex = cm.getMatchIndex() / 2;
                TournamentMatch nextMatch = nextRoundMatches.stream().filter(m -> m.getMatchIndex() == nextIndex).findFirst().orElse(null);
                if(nextMatch != null) {
                    cm.setNextMatchId(nextMatch.getId());
                    matchRepository.save(cm);
                }
            }
        }

        return matches;
    }

    private List<TournamentMatch> generateRoundRobin(Tournament tournament, List<Team> teams) {
        List<TournamentMatch> matches = new ArrayList<>();
        List<Team> localTeams = new ArrayList<>(teams);

        if (localTeams.size() % 2 != 0) {
            localTeams.add(null); // BYE
        }
        
        int numDays = localTeams.size() - 1;
        int halfSize = localTeams.size() / 2;

        for (int day = 0; day < numDays; day++) {
            for (int i = 0; i < halfSize; i++) {
                Team teamA = localTeams.get(i);
                Team teamB = localTeams.get(localTeams.size() - 1 - i);

                if (teamA != null && teamB != null) {
                    TournamentMatch match = TournamentMatch.builder()
                            .tournament(tournament)
                            .round(day + 1)
                            .matchIndex(i)
                            .teamA(teamA)
                            .teamB(teamB)
                            .status(MatchStatus.SCHEDULED)
                            .build();
                    matches.add(matchRepository.save(match));
                }
            }
            localTeams.add(1, localTeams.remove(localTeams.size() - 1));
        }
        return matches;
    }

    @Transactional
    public TournamentMatch submitResult(SubmitResultRequest request) {
        TournamentMatch match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new IllegalStateException("Match already finished");
        }

        match.setScoreA(request.getScoreA());
        match.setScoreB(request.getScoreB());

        if (match.getTournament().getFormat() == TournamentFormat.SINGLE_ELIMINATION) {
            Team winner = request.getScoreA() >= request.getScoreB() ? match.getTeamA() : match.getTeamB();
            match.setWinner(winner);
            match.setStatus(MatchStatus.FINISHED);
            matchRepository.save(match);
            advanceWinnerSingleElimination(match, winner);
        } else {
            Team winner = null;
            if(request.getScoreA() > request.getScoreB()) winner = match.getTeamA();
            else if(request.getScoreB() > request.getScoreA()) winner = match.getTeamB();
            match.setWinner(winner);
            match.setStatus(MatchStatus.FINISHED);
            matchRepository.save(match);
            updateLeaderboard(match);
        }

        return match;
    }

    private void updateLeaderboard(TournamentMatch match) {
        UUID tourId = match.getTournament().getId();
        LeaderboardEntry entryA = leaderboardRepository.findByTournamentIdAndTeamId(tourId, match.getTeamA().getId()).orElseThrow();
        LeaderboardEntry entryB = leaderboardRepository.findByTournamentIdAndTeamId(tourId, match.getTeamB().getId()).orElseThrow();

        entryA.setGoalsFor(entryA.getGoalsFor() + match.getScoreA());
        entryA.setGoalsAgainst(entryA.getGoalsAgainst() + match.getScoreB());
        entryB.setGoalsFor(entryB.getGoalsFor() + match.getScoreB());
        entryB.setGoalsAgainst(entryB.getGoalsAgainst() + match.getScoreA());

        if (match.getScoreA() > match.getScoreB()) {
            entryA.setWins(entryA.getWins() + 1);
            entryA.setPoints(entryA.getPoints() + 3);
            entryB.setLosses(entryB.getLosses() + 1);
        } else if (match.getScoreB() > match.getScoreA()) {
            entryB.setWins(entryB.getWins() + 1);
            entryB.setPoints(entryB.getPoints() + 3);
            entryA.setLosses(entryA.getLosses() + 1);
        } else {
            entryA.setDraws(entryA.getDraws() + 1);
            entryA.setPoints(entryA.getPoints() + 1);
            entryB.setDraws(entryB.getDraws() + 1);
            entryB.setPoints(entryB.getPoints() + 1);
        }

        leaderboardRepository.save(entryA);
        leaderboardRepository.save(entryB);
    }

    public List<TournamentMatch> getMatches(UUID tournamentId) {
        return matchRepository.findByTournamentIdOrderByRoundAscMatchIndexAsc(tournamentId);
    }

    public List<LeaderboardEntry> getLeaderboard(UUID tournamentId) {
        return leaderboardRepository.findByTournamentIdOrderByPointsDescGoalsForDesc(tournamentId);
    }

    private void advanceWinnerSingleElimination(TournamentMatch finishedMatch, Team winner) {
        List<TournamentMatch> nextRoundMatches = matchRepository.findByTournamentIdAndRound(
                finishedMatch.getTournament().getId(), finishedMatch.getRound() + 1);

        if (nextRoundMatches.isEmpty()) {
            finishedMatch.getTournament().setStatus("FINISHED");
            tournamentRepository.save(finishedMatch.getTournament());
            return;
        }

        int nextMatchIndex = finishedMatch.getMatchIndex() / 2;
        TournamentMatch nextMatch = nextRoundMatches.stream()
                .filter(m -> m.getMatchIndex() == nextMatchIndex)
                .findFirst()
                .orElseThrow();

        if (finishedMatch.getMatchIndex() % 2 == 0) {
            nextMatch.setTeamA(winner);
        } else {
            nextMatch.setTeamB(winner);
        }
        matchRepository.save(nextMatch);
    }

    private int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) power *= 2;
        return power;
    }
}
