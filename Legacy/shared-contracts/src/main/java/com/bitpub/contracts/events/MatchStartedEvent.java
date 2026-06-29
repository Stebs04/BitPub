package com.bitpub.contracts.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class MatchStartedEvent extends BaseSensorEvent {
    
    private String matchMode; // e.g., "1v1", "2v2"
    
    private List<String> players; // IDs or names of players involved
}
