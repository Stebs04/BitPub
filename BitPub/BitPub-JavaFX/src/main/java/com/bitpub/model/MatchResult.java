package com.bitpub.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO per il risultato di una partita. Allineato al backend stats-service.
 * Il backend usa winner/loser (non player1/player2).
 */
public class MatchResult {
    private UUID id;
    private UUID matchSessionId;
    private UUID gameId;
    private UUID winnerUserId;
    private UUID loserUserId;
    private int winnerScore;
    private int loserScore;
    private LocalDateTime playedAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMatchSessionId() { return matchSessionId; }
    public void setMatchSessionId(UUID matchSessionId) { this.matchSessionId = matchSessionId; }

    public UUID getGameId() { return gameId; }
    public void setGameId(UUID gameId) { this.gameId = gameId; }

    public UUID getWinnerUserId() { return winnerUserId; }
    public void setWinnerUserId(UUID winnerUserId) { this.winnerUserId = winnerUserId; }

    public UUID getLoserUserId() { return loserUserId; }
    public void setLoserUserId(UUID loserUserId) { this.loserUserId = loserUserId; }

    public int getWinnerScore() { return winnerScore; }
    public void setWinnerScore(int winnerScore) { this.winnerScore = winnerScore; }

    public int getLoserScore() { return loserScore; }
    public void setLoserScore(int loserScore) { this.loserScore = loserScore; }

    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
}
