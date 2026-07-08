package it.uniupo.pissir.bitpub.statisticsservice.repository;

import it.uniupo.pissir.bitpub.statisticsservice.domain.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /** Returns all entries for a game type within a single locale, ordered by wins DESC then totalPoints DESC. */
    List<Leaderboard> findByGameTypeIdAndLocaleIdOrderByWinsDescTotalPointsDesc(String gameTypeId, String localeId);

    /** Tutte le entry registrate in un locale (per aggregare i giochi piu' usati). */
    List<Leaderboard> findByLocaleId(String localeId);

    /** gameTypeId distinti su cui il giocatore ha almeno una entry (statistiche personali dinamiche). */
    @Query("SELECT DISTINCT l.gameTypeId FROM Leaderboard l WHERE l.playerName = :playerName")
    List<String> findDistinctGameTypeIdsByPlayerName(String playerName);
}
