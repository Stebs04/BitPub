package it.uniupo.pissir.bitpub.tournamentservice.repository;

import it.uniupo.pissir.bitpub.tournamentservice.domain.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, String> {
    List<TournamentMatch> findByTournamentIdOrderByRoundAscMatchIndexAsc(String tournamentId);
}
