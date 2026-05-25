package com.bitpub.model;

import java.util.UUID;

public class MatchModel {
    private UUID id;
    private UUID tournamentId;
    private int round;
    private int matchIndex;
    private TeamModel teamA;
    private TeamModel teamB;
    private TeamModel winner;
    private int scoreA;
    private int scoreB;
    private UUID nextMatchId;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTournamentId() { return tournamentId; }
    public void setTournamentId(UUID tournamentId) { this.tournamentId = tournamentId; }
    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }
    public int getMatchIndex() { return matchIndex; }
    public void setMatchIndex(int matchIndex) { this.matchIndex = matchIndex; }
    public TeamModel getTeamA() { return teamA; }
    public void setTeamA(TeamModel teamA) { this.teamA = teamA; }
    public TeamModel getTeamB() { return teamB; }
    public void setTeamB(TeamModel teamB) { this.teamB = teamB; }
    public TeamModel getWinner() { return winner; }
    public void setWinner(TeamModel winner) { this.winner = winner; }
    public int getScoreA() { return scoreA; }
    public void setScoreA(int scoreA) { this.scoreA = scoreA; }
    public int getScoreB() { return scoreB; }
    public void setScoreB(int scoreB) { this.scoreB = scoreB; }
    public UUID getNextMatchId() { return nextMatchId; }
    public void setNextMatchId(UUID nextMatchId) { this.nextMatchId = nextMatchId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
