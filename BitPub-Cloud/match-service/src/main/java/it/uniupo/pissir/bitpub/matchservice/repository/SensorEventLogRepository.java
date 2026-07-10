// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.repository;

import it.uniupo.pissir.bitpub.matchservice.domain.SensorEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorEventLogRepository extends JpaRepository<SensorEventLog, String> {
    boolean existsByEventId(String eventId);
}
