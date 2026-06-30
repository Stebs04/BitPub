package it.uniupo.pissir.bitpub.statisticsservice.repository;

import it.uniupo.pissir.bitpub.statisticsservice.domain.AggregateStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AggregateStatisticRepository extends JpaRepository<AggregateStatistic, String> {
    List<AggregateStatistic> findByEntityIdAndEntityType(String entityId, String entityType);
    Optional<AggregateStatistic> findByEntityIdAndEntityTypeAndMetricName(String entityId, String entityType, String metricName);
}
