/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.repository;

import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository dedicato alla gestione delle classifiche dei tornei.
 * Fornisce metodi per recuperare la classifica ordinata secondo diversi criteri.
 */
@Repository
public interface TournamentRankingRepository extends JpaRepository<TournamentRanking, String> {
    
    // Ottiene la classifica di un torneo basandosi sul punteggio decrescente
    List<TournamentRanking> findByTournamentIdOrderByScoreDesc(String tournamentId);
    
    // Ottiene la classifica di un torneo ordinata per numero di vittorie
    List<TournamentRanking> findByTournamentIdOrderByMatchesWonDesc(String tournamentId);
    
    // Trova la posizione in classifica di uno specifico partecipante all'interno del torneo
    Optional<TournamentRanking> findByTournamentIdAndParticipantId(String tournamentId, String participantId);
}
