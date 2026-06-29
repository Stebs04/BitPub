package com.bitpub.repository;

import com.bitpub.models.PartitaFreccette;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository per la gestione della persistenza delle partite di Freccette.
 * @author Timothy Giolito
 */
@Repository
public interface PartitaFreccetteRepository extends JpaRepository<PartitaFreccette, Long> {
    
    @Query("SELECT p FROM PartitaFreccette p WHERE p.localeId = :localeId AND (:stato = 'IN_CORSO' AND p.orarioFine IS NULL)")
    List<PartitaFreccette> findByLocaleIdAndStato(@Param("localeId") Long localeId, @Param("stato") String stato);
    
    @Query("SELECT COUNT(p) FROM PartitaFreccette p WHERE p.localeId = :id AND p.orarioInizio >= :today")
    long countToday(@Param("id") Long id, @Param("today") LocalDateTime today);

    @Query("SELECT p FROM PartitaFreccette p WHERE p.localeId = :localeId AND p.orarioFine IS NOT NULL")
    List<PartitaFreccette> findConcluseByLocaleId(@Param("localeId") Long localeId);

    /**
     * Calcola la durata media (in minuti) delle partite concluse per uno specifico locale.
     */
    default Double calculateAverageDuration(Long id) {
        List<PartitaFreccette> partite = findConcluseByLocaleId(id);
        if (partite.isEmpty()) return null;
        return partite.stream()
                .mapToDouble(p -> java.time.Duration.between(p.getOrarioInizio(), p.getOrarioFine()).getSeconds() / 60.0)
                .average()
                .orElse(0.0);
    }
}