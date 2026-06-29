package com.bitpub.model;

import java.util.UUID;

/**
 * DTO per la voce della leaderboard globale. Allineato al backend LeaderboardEntryDto.
 * Campi del backend: rank, userId, username, wins, losses, totalMatches, totalScore, winRate
 */
public class LeaderboardEntryDto {
    private int rank;
    private UUID userId;
    private String username;
    private int wins;
    private int losses;
    private int totalMatches;
    private int totalScore;
    private double winRate;

    // Getters and Setters
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }
}
