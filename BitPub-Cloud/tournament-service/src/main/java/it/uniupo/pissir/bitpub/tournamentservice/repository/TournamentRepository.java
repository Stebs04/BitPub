package it.uniupo.pissir.bitpub.tournamentservice.repository;

import it.uniupo.pissir.bitpub.tournamentservice.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, String> {
    List<Tournament> findByStatus(String status);
    List<Tournament> findByGameTypeId(String gameTypeId);
}
