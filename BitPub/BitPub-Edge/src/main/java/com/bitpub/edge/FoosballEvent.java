package com.bitpub.edge;

import java.time.LocalDateTime;

/**
 * POJO immutabile per la serializzazione GSON degli eventi di gioco generati dall'Edge.
 * Include sessionId, rullate e durata media pallina.
 */
public class FoosballEvent {
    private final int    tableId;
    private final Long   sessionId;
    private final String eventType;          // GOAL | FALLO | START | FORCE_STOPPED
    private final int    scoreBlue;
    private final int    scoreRed;
    private final String status;
    private final String winner;
    private final int    totaleRullate;
    private final int    durataMediaPallinaSecondi;
    private final String timestamp;

    public FoosballEvent(int tableId, Long sessionId, String eventType,
                         int scoreBlue, int scoreRed, String status, String winner,
                         int totaleRullate, int durataMediaPallinaSecondi) {
        this.tableId                    = tableId;
        this.sessionId                  = sessionId;
        this.eventType                  = eventType;
        this.scoreBlue                  = scoreBlue;
        this.scoreRed                   = scoreRed;
        this.status                     = status;
        this.winner                     = winner;
        this.totaleRullate              = totaleRullate;
        this.durataMediaPallinaSecondi  = durataMediaPallinaSecondi;
        this.timestamp                  = LocalDateTime.now().toString();
    }

    public int    getTableId()                     { return tableId; }
    public Long   getSessionId()                   { return sessionId; }
    public String getEventType()                   { return eventType; }
    public int    getScoreBlue()                   { return scoreBlue; }
    public int    getScoreRed()                    { return scoreRed; }
    public String getStatus()                      { return status; }
    public String getWinner()                      { return winner; }
    public int    getTotaleRullate()               { return totaleRullate; }
    public int    getDurataMediaPallinaSecondi()   { return durataMediaPallinaSecondi; }
    public String getTimestamp()                   { return timestamp; }
}
