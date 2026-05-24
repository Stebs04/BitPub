package com.bitpub.tournament.repository;

import com.bitpub.tournament.model.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, UUID> {
    List<TournamentMatch> findByTournamentIdOrderByRoundAscMatchIndexAsc(UUID tournamentId);
    List<TournamentMatch> findByTournamentIdAndRound(UUID tournamentId, int round);
}
