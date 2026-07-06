package it.uniupo.pissir.bitpub.statisticsservice.service;

import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.GlobalStatsDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.LeaderboardEntryDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.MatchResultEvent;
import it.uniupo.pissir.bitpub.statisticsservice.dto.StatisticUpdateRequest;

import java.util.List;

public interface StatisticsService {
    List<AggregateStatisticDto> getStatisticsByEntity(String entityId, String entityType);
    AggregateStatisticDto updateStatistic(StatisticUpdateRequest request);

    /** Records a match result and updates the leaderboard for winner and loser. */
    void recordMatchResult(MatchResultEvent event);

    /** Reset e ricostruzione della leaderboard dagli eventi dei match gia' conclusi. Ritorna il numero applicato. */
    int rebuildLeaderboard(List<MatchResultEvent> events);

    /** Returns leaderboard entries for a specific game type, ordered by wins then points. */
    List<LeaderboardEntryDto> getLeaderboard(String gameTypeId);

    /** Returns leaderboard entries for a specific game type, scoped to a single locale. */
    List<LeaderboardEntryDto> getLeaderboardByLocale(String gameTypeId, String localeId);

    /** Returns the localeId of the locale owned by the given adminId, or null if none. */
    String resolveAdminLocaleId(String adminId);

    /** Aggregates platform-wide monitoring data for the PLATFORM_ADMIN dashboard. */
    GlobalStatsDto getGlobalOverview();
}

