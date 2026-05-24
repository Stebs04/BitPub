package com.bitpub.stats.repository;

import com.bitpub.stats.model.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, UUID> {
    List<MatchResult> findByWinnerUserIdOrLoserUserIdOrderByPlayedAtDesc(UUID winnerUserId, UUID loserUserId);
    boolean existsByMatchSessionId(UUID matchSessionId);
}
