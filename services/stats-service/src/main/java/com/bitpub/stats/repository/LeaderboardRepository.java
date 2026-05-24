package com.bitpub.stats.repository;

import com.bitpub.stats.model.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, UUID> {
    Optional<Leaderboard> findByUserIdAndGameId(UUID userId, UUID gameId);
    List<Leaderboard> findByGameIdOrderByScoreDesc(UUID gameId);
}
