package com.bitpub.model;

import java.util.UUID;

public class LeaderboardEntryModel {
    private UUID id;
    private UUID tournamentId;
    private TeamModel team;
    private int points;
    private int wins;
    private int losses;
    private int draws;
    private int goalsFor;
    private int goalsAgainst;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTournamentId() { return tournamentId; }
    public void setTournamentId(UUID tournamentId) { this.tournamentId = tournamentId; }
    public TeamModel getTeam() { return team; }
    public void setTeam(TeamModel team) { this.team = team; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }
    public int getGoalsFor() { return goalsFor; }
    public void setGoalsFor(int goalsFor) { this.goalsFor = goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public void setGoalsAgainst(int goalsAgainst) { this.goalsAgainst = goalsAgainst; }

    public String getTeamName() {
        return team != null ? team.getName() : "";
    }
}
