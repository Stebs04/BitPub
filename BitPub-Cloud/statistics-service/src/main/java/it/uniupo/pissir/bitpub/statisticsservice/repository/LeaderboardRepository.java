package it.uniupo.pissir.bitpub.statisticsservice.repository;

import it.uniupo.pissir.bitpub.statisticsservice.domain.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, String> {

    /** Returns all entries for a game type, ordered by wins DESC then totalPoints DESC. */
    List<Leaderboard> findByGameTypeIdOrderByWinsDescTotalPointsDesc(String gameTypeId);

    /** Find a specific player's entry for a given game type. */
    Optional<Leaderboard> findByPlayerNameIgnoreCaseAndGameTypeId(String playerName, String gameTypeId);

    /** Returns top-N players across all game types (for a global leaderboard if needed). */
    List<Leaderboard> findTop10ByOrderByWinsDescTotalPointsDesc();
}
