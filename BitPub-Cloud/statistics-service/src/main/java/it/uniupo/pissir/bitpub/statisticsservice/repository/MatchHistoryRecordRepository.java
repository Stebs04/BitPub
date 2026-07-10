/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.repository;

import it.uniupo.pissir.bitpub.statisticsservice.domain.MatchHistoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchHistoryRecordRepository extends JpaRepository<MatchHistoryRecord, String> {

    /** Metodo di controllo fondamentale per garantire l'idempotenza durante l'ingestion MQTT: previene doppi inserimenti per lo stesso match. */
    boolean existsByMatchId(String matchId);
}
