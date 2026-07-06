package it.uniupo.pissir.bitpub.tournamentservice.service;

import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRegistrationDto;

import java.util.List;

public interface TournamentService {
    TournamentDto createTournament(TournamentDto tournamentDto);
    TournamentDto updateTournament(String id, TournamentDto tournamentDto);
    void deleteTournament(String id);
    TournamentDto getTournament(String id);
    List<TournamentDto> getAllTournaments();
    List<TournamentDto> getActiveTournaments();
    TournamentDto startTournament(String id);
    TournamentDto endTournament(String id);
    TournamentRegistrationDto registerToTournament(String tournamentId, TournamentRegistrationDto registrationDto);
    TournamentDto generateBracket(String tournamentId);
    TournamentDto updateMatchResult(String matchId, String winnerId, String stats);
    boolean isPlayerInBracketMatch(String matchId, String playerId);
}
