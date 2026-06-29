package it.uniupo.pissir.bitpub.statisticsservice.service;

import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.StatisticUpdateRequest;

import java.util.List;

public interface StatisticsService {
    List<AggregateStatisticDto> getStatisticsByEntity(String entityId, String entityType);
    AggregateStatisticDto updateStatistic(StatisticUpdateRequest request);
}
