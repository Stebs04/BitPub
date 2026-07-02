package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

/** Incoming DTO from match-service when a match completes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultEvent {
    private String gameTypeId;
    private String winnerName;
    private String loserName;
    private int winnerScore;
    private int loserScore;
    private String matchId;
    private String localeId;
}
