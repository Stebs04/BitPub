package it.uniupo.pissir.bitpub.tournamentservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRankingDto {
    private String id;
    private String tournamentId;
    private String participantId;
    private String participantName;
    private int goalsScored;
    private int matchesPlayed;
    private int matchesWon;
    private int currentRank;
}
