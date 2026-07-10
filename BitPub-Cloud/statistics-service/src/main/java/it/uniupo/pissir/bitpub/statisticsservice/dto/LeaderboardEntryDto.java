/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.dto;

import lombok.*;

import java.time.Instant;

/** Rappresentazione di una singola voce all'interno della classifica (Leaderboard). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDto {
    private String id;
    private String playerName;
    private String gameTypeId;
    private String localeId;
    private boolean teamBased;
    private int wins;
    private int losses;
    private int totalPoints;
    private int matchesPlayed;
    private Instant lastUpdated;
}
