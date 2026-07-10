/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.repository;

import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Interfaccia repository per il salvataggio e il recupero dei singoli incontri del torneo.
 */
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, String> {
    
    // Recupera tutti gli incontri di un determinato torneo, ordinati cronologicamente per turno e posizione nel tabellone
    List<TournamentMatch> findByTournamentIdOrderByRoundAscMatchIndexAsc(String tournamentId);
}
