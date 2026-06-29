package it.uniupo.pissir.bitpub.statisticsservice.service.impl;

import it.uniupo.pissir.bitpub.statisticsservice.domain.AggregateStatistic;
import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.StatisticUpdateRequest;
import it.uniupo.pissir.bitpub.statisticsservice.repository.AggregateStatisticRepository;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final AggregateStatisticRepository statisticRepository;

    @Override
    public List<AggregateStatisticDto> getStatisticsByEntity(String entityId, String entityType) {
        return statisticRepository.findByEntityIdAndEntityType(entityId, entityType).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AggregateStatisticDto updateStatistic(StatisticUpdateRequest request) {
        Optional<AggregateStatistic> optionalStat = statisticRepository
                .findByEntityIdAndEntityTypeAndMetricName(
                        request.getEntityId(), request.getEntityType(), request.getMetricName());
        
        AggregateStatistic stat;
        if (optionalStat.isPresent()) {
            stat = optionalStat.get();
            if (request.isAbsolute()) {
                stat.setMetricValue(request.getDeltaValue());
            } else {
                stat.setMetricValue(stat.getMetricValue() + request.getDeltaValue());
            }
        } else {
            stat = AggregateStatistic.builder()
                    .entityId(request.getEntityId())
                    .entityType(request.getEntityType())
                    .metricName(request.getMetricName())
                    .metricValue(request.getDeltaValue())
                    .build();
        }
        
        stat.setLastComputedAt(Instant.now());
        stat = statisticRepository.save(stat);
        
        return mapToDto(stat);
    }

    private AggregateStatisticDto mapToDto(AggregateStatistic stat) {
        return AggregateStatisticDto.builder()
                .id(stat.getId())
                .entityId(stat.getEntityId())
                .entityType(stat.getEntityType())
                .metricName(stat.getMetricName())
                .metricValue(stat.getMetricValue())
                .lastComputedAt(stat.getLastComputedAt())
                .build();
    }
}
