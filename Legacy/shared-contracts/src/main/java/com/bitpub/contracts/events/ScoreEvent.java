package com.bitpub.contracts.events;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class ScoreEvent extends BaseSensorEvent {
    
    @Min(0)
    private int scoreTeamA;
    
    @Min(0)
    private int scoreTeamB;
    
    // Who caused the score change
    private String scoringTeam;
}
