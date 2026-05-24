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
public class RecordMatchRequest {
    private UUID matchSessionId;
    private UUID gameId;
    private UUID winnerUserId;
    private String winnerUsername;
    private UUID loserUserId;
    private String loserUsername;
    private int winnerScore;
    private int loserScore;
}
