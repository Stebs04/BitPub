package com.bitpub.stats.repository;

import com.bitpub.stats.model.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, UUID> {

    Optional<PlayerStats> findByUserIdAndGameId(UUID userId, UUID gameId);

    @Query("SELECT p FROM PlayerStats p WHERE p.gameId = :gameId ORDER BY p.wins DESC, p.totalScore DESC")
    List<PlayerStats> findLeaderboardByGame(UUID gameId);

    @Query("SELECT p FROM PlayerStats p ORDER BY p.wins DESC, p.totalScore DESC")
    List<PlayerStats> findGlobalLeaderboard();
}
