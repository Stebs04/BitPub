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
public class LeaderboardEntryDto {
    private int rank;
    private UUID userId;
    private String username;
    private int wins;
    private int losses;
    private int totalMatches;
    private int totalScore;
    private double winRate;
}
