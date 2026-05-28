package com.bitpub.contracts.events;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class GoalEvent extends BaseSensorEvent {
    
    @NotBlank
    private String team;
    
    private String player; // Optional: depending on sensor capability
    
    private boolean isOwnGoal;
}
