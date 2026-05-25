package com.bitpub.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedPlayerStatsDto {
    private UUID userId;
    private String username;
    private UUID gameId;
    private int totalMatches;
    private int wins;
    private int losses;
    private int totalScore;
    private double winRate;
    private long rank; // Can be global or per game
}
