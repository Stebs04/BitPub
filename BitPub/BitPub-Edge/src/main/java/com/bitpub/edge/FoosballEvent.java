package com.bitpub.edge;

import java.time.LocalDateTime;

/**
 * FILE 19: FoosballEvent
 * POJO immutabile per la serializzazione GSON degli eventi di gioco.
 */
public class FoosballEvent {
    private final int tableId;
    private final String eventType;
    private final int scoreBlue;
    private final int scoreRed;
    private final String status;
    private final String winner;
    private final String timestamp;

    public FoosballEvent(int tableId, String eventType, int scoreBlue, int scoreRed, String status, String winner) {
        this.tableId = tableId;
        this.eventType = eventType;
        this.scoreBlue = scoreBlue;
        this.scoreRed = scoreRed;
        this.status = status;
        this.winner = winner;
        this.timestamp = LocalDateTime.now().toString();
    }

    public int getTableId() { return tableId; }
    public String getEventType() { return eventType; }
    public int getScoreBlue() { return scoreBlue; }
    public int getScoreRed() { return scoreRed; }
    public String getStatus() { return status; }
    public String getWinner() { return winner; }
    public String getTimestamp() { return timestamp; }
}
