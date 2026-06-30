package it.uniupo.pissir.bitpub.matchservice.dto;

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
    private String gameTypeId;
    private String status; // e.g. WAITING, PLAYING, FINISHED
    private String teamAName;  // Display name of team/player A
    private String teamBName;  // Display name of team/player B
    private int scoreTeamA;
    private int scoreTeamB;
    private int timeRemainingSeconds;
    private String currentEventMessage; // e.g. "GOAL!", "MATCH STARTED"
    private String winnerName;          // Populated on MATCH_END
}
