package it.uniupo.pissir.bitpub.statisticsservice.service;

import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.LeaderboardEntryDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.MatchResultEvent;
import it.uniupo.pissir.bitpub.statisticsservice.dto.StatisticUpdateRequest;

import java.util.List;

public interface StatisticsService {
    List<AggregateStatisticDto> getStatisticsByEntity(String entityId, String entityType);
    AggregateStatisticDto updateStatistic(StatisticUpdateRequest request);

    /** Records a match result and updates the leaderboard for winner and loser. */
    void recordMatchResult(MatchResultEvent event);

    /** Returns leaderboard entries for a specific game type, ordered by wins then points. */
    List<LeaderboardEntryDto> getLeaderboard(String gameTypeId);
}

