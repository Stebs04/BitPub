package com.bitpub.tournament.repository;

import com.bitpub.tournament.model.TournamentPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TournamentPlayerRepository extends JpaRepository<TournamentPlayer, UUID> {
    List<TournamentPlayer> findByTeamId(UUID teamId);
}
