/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.service;

import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRankingDto;

import java.util.List;

/**
 * Interfaccia del servizio che gestisce la logica di calcolo e aggiornamento delle classifiche dei tornei.
 */
public interface TournamentRankingService {
    
    // Recupera la classifica aggiornata per un determinato torneo
    List<TournamentRankingDto> getTournamentRankings(String tournamentId);
    
    // Aggiorna il punteggio di un partecipante in seguito all'esito di un match
    TournamentRankingDto updateRankingScore(String tournamentId, String participantId, int scoreDelta, boolean isWin);
    
    // Inizializza i record di classifica per tutti i partecipanti iscritti prima dell'inizio del torneo
    void initializeRankingsForTournament(String tournamentId);
}
