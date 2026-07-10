/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.repository;

import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository responsabile per l'accesso ai dati relativi alle iscrizioni ai tornei.
 */
@Repository
public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, String> {
    
    // Restituisce l'elenco completo delle iscrizioni per un determinato torneo
    List<TournamentRegistration> findByTournamentId(String tournamentId);
    
    // Verifica rapidamente se un giocatore o squadra è già iscritto alla competizione
    boolean existsByTournamentIdAndParticipantId(String tournamentId, String participantId);
    
    // Recupera l'iscrizione specifica di un partecipante, se presente
    Optional<TournamentRegistration> findByTournamentIdAndParticipantId(String tournamentId, String participantId);
}
