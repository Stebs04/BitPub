package it.uniupo.pissir.bitpub.tournamentservice.repository;

import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, String> {
    List<TournamentRegistration> findByTournamentId(String tournamentId);
    boolean existsByTournamentIdAndParticipantId(String tournamentId, String participantId);
    Optional<TournamentRegistration> findByTournamentIdAndParticipantId(String tournamentId, String participantId);
}
