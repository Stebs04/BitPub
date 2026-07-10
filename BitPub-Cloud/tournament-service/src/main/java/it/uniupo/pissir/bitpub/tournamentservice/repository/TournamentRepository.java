/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.repository;

import it.uniupo.pissir.bitpub.tournamentservice.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Punto di accesso principale per le operazioni sul database riguardanti i tornei.
 */
@Repository
public interface TournamentRepository extends JpaRepository<Tournament, String> {
    
    // Cerca tutti i tornei che si trovano in uno stato specifico (es. UPCOMING, ACTIVE, COMPLETED)
    List<Tournament> findByStatus(String status);
    
    // Filtra i tornei disponibili in base alla tipologia di gioco
    List<Tournament> findByGameTypeId(String gameTypeId);
}
