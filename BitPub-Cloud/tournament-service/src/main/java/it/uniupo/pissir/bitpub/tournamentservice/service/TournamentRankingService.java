package it.uniupo.pissir.bitpub.tournamentservice.service;

import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRankingDto;

import java.util.List;

public interface TournamentRankingService {
    List<TournamentRankingDto> getTournamentRankings(String tournamentId);
    TournamentRankingDto updateRankingScore(String tournamentId, String participantId, int scoreDelta, boolean isWin);
    void initializeRankingsForTournament(String tournamentId);
}
