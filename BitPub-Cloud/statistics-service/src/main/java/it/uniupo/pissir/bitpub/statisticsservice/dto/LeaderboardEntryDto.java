package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

import java.time.Instant;

/** Public DTO representing a leaderboard entry returned by the API. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDto {
    private String id;
    private String playerName;
    private String gameTypeId;
    private int wins;
    private int losses;
    private int totalPoints;
    private int matchesPlayed;
    private Instant lastUpdated;
}
