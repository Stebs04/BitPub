package com.bitpub.repository;

import com.bitpub.models.PartitaBiliardo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository per la gestione della persistenza delle partite di Biliardo.
 * @author Luca Franzon
 */
@Repository
public interface PartitaBiliardoRepository extends JpaRepository<PartitaBiliardo, Long> {
    @Query("SELECT p FROM PartitaBiliardo p WHERE p.localeId = :localeId AND (:stato = 'IN_CORSO' AND p.orarioFine IS NULL)")
    List<PartitaBiliardo> findByLocaleIdAndStato(@Param("localeId") Long localeId, @Param("stato") String stato);
    
    @Query("SELECT COUNT(p) FROM PartitaBiliardo p WHERE p.localeId = :id AND p.orarioInizio >= :today")
    long countToday(@Param("id") Long id, @Param("today") LocalDateTime today);

    @Query("SELECT p FROM PartitaBiliardo p WHERE p.localeId = :localeId AND p.orarioFine IS NOT NULL")
    List<PartitaBiliardo> findConcluseByLocaleId(@Param("localeId") Long localeId);

    default Double calculateAverageDuration(Long id) {
        List<PartitaBiliardo> partite = findConcluseByLocaleId(id);
        if (partite.isEmpty()) return null;
        return partite.stream()
                .mapToDouble(p -> java.time.Duration.between(p.getOrarioInizio(), p.getOrarioFine()).getSeconds() / 60.0)
                .average()
                .orElse(0.0);
    }
}
