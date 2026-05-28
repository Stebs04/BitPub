package com.bitpub.tournament.repository;

import com.bitpub.tournament.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByTournamentId(UUID tournamentId);
}
