/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.service;

import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRegistrationDto;

import java.util.List;

/**
 * Interfaccia principale per la gestione del ciclo di vita dei tornei.
 * Espone le operazioni per creare, gestire le iscrizioni, avviare il torneo e processare i risultati.
 */
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
