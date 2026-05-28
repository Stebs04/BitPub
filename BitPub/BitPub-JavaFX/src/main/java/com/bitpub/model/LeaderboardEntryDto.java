package com.bitpub.model;

import java.util.UUID;

public class LeaderboardEntryDto {
    private UUID userId;
    private String username;
    private int totalMatches;
    private int totalWins;
    private int totalLosses;
    private double winRate;
    private int eloScore;
    
    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public int getTotalMatches() { return totalMatches; }
    public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
    
    public int getTotalWins() { return totalWins; }
    public void setTotalWins(int totalWins) { this.totalWins = totalWins; }
    
    public int getTotalLosses() { return totalLosses; }
    public void setTotalLosses(int totalLosses) { this.totalLosses = totalLosses; }
    
    public double getWinRate() { return winRate; }
    public void setWinRate(double winRate) { this.winRate = winRate; }
    
    public int getEloScore() { return eloScore; }
    public void setEloScore(int eloScore) { this.eloScore = eloScore; }
}
