package it.uniupo.pissir.bitpub.javafx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateDto {
    private String matchId;
    private String status; // e.g. WAITING, PLAYING, FINISHED
    private int scoreTeamA;
    private int scoreTeamB;
    private int timeRemainingSeconds;
    private String currentEventMessage; // e.g. "GOAL!", "MATCH STARTED"
}
