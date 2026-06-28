package com.bitpub.stats.repository;

import com.bitpub.stats.model.PlayerStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, UUID>, JpaSpecificationExecutor<PlayerStats> {

    Optional<PlayerStats> findByUserIdAndGameId(UUID userId, UUID gameId);

    @Query("SELECT p FROM PlayerStats p WHERE p.gameId = :gameId ORDER BY p.wins DESC, p.totalScore DESC")
    Page<PlayerStats> findLeaderboardByGame(@Param("gameId") UUID gameId, Pageable pageable);

    @Query("SELECT p FROM PlayerStats p ORDER BY p.wins DESC, p.totalScore DESC")
    Page<PlayerStats> findGlobalLeaderboard(Pageable pageable);

    @Query(value = "SELECT rank_num FROM (SELECT user_id, DENSE_RANK() OVER (ORDER BY wins DESC, total_score DESC) as rank_num FROM stats.player_stats) ranked WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Long findGlobalRankByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT rank_num FROM (SELECT user_id, DENSE_RANK() OVER (ORDER BY wins DESC, total_score DESC) as rank_num FROM stats.player_stats WHERE game_id = :gameId) ranked WHERE user_id = :userId LIMIT 1", nativeQuery = true)
    Long findGameRankByUserId(@Param("userId") UUID userId, @Param("gameId") UUID gameId);
}
