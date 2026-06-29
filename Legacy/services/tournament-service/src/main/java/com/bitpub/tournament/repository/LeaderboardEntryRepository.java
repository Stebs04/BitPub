package com.bitpub.tournament.repository;

import com.bitpub.tournament.model.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, UUID> {
    List<LeaderboardEntry> findByTournamentIdOrderByPointsDescGoalsForDesc(UUID tournamentId);
    Optional<LeaderboardEntry> findByTournamentIdAndTeamId(UUID tournamentId, UUID teamId);
}
