package com.bitpub.contracts.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class MatchEndedEvent extends BaseSensorEvent {
    
    private String winningTeam;
    
    private int finalScoreTeamA;
    
    private int finalScoreTeamB;
    
    private String matchDuration; // e.g., in ISO-8601 duration format or seconds
}
