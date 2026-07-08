package it.uniupo.pissir.bitpub.statisticsservice.repository;

import it.uniupo.pissir.bitpub.statisticsservice.domain.MatchHistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchHistoryRecordRepository extends JpaRepository<MatchHistoryRecord, String> {

    /** Idempotenza dell'ingest MQTT: una riconsegna QoS1 dello stesso match viene ignorata. */
    boolean existsByMatchId(String matchId);
}
