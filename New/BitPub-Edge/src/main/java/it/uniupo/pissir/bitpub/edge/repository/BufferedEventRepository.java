package it.uniupo.pissir.bitpub.edge.repository;

import it.uniupo.pissir.bitpub.edge.model.BufferedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BufferedEventRepository extends JpaRepository<BufferedEvent, Long> {
    List<BufferedEvent> findAllByOrderByCreatedAtAsc();
    boolean existsByOriginalEventId(UUID originalEventId);
}
