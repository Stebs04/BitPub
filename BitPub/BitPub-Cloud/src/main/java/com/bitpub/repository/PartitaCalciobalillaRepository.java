package com.bitpub.repository;

import com.bitpub.models.PartitaCalciobalilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Import necessario per l'uso di @Param
import org.springframework.stereotype.Repository;

/**
 * Interfaccia di persistenza per l'entità {@link PartitaCalciobalilla}.
 * Estende {@link JpaRepository} per fornire le operazioni CRUD standard e definisce
 * query personalizzate (JPQL) per il calcolo delle statistiche globali e specifiche del locale.
 *
 * @author Stefano Bellan 20054330
 */
@Repository
public interface PartitaCalciobalillaRepository extends JpaRepository<PartitaCalciobalilla, Long> {

    /**
     * Calcola il numero aggregato di rullate effettuate in tutte le partite registrate.
     *
     * @return La somma totale delle rullate come {@link Integer}.
     */
    @Query("SELECT SUM(p.totaleRullate) FROM PartitaCalciobalilla p")
    Integer countTotalRullate();

    /**
     * Conta il numero totale di match in cui la squadra Rossa ha segnato più gol della Blu.
     *
     * @return Il numero complessivo di vittorie della squadra Rossa.
     */
    @Query("SELECT COUNT(p) FROM PartitaCalciobalilla p WHERE p.goalRossi > p.goalBlu")
    Integer countVittorieRossi();

    /**
     * Conta il numero totale di match in cui la squadra Blu ha superato la squadra Rossa.
     *
     * @return Il numero complessivo di vittorie della squadra Blu.
     */
    @Query("SELECT COUNT(p) FROM PartitaCalciobalilla p WHERE p.goalBlu > p.goalRossi")
    Integer countVittorieBlu();

    /**
     * Recupera le partite concluse all'interno di un determinato locale.
     */
    @Query("SELECT p FROM PartitaCalciobalilla p WHERE p.localeId = :localeId AND p.orarioFine IS NOT NULL")
    java.util.List<PartitaCalciobalilla> findConcluseByLocaleId(@Param("localeId") Long localeId);

    /**
     * Calcola la durata media (in minuti) delle partite di calciobalilla completate 
     * all'interno di un determinato locale.
     * La query estrae la differenza in secondi tra l'inizio e la fine della partita,
     * convertendola successivamente in minuti per facilitarne la lettura nella dashboard.
     *
     * @param localeId L'ID univoco del locale di cui si vogliono calcolare le statistiche.
     * @return La durata media delle partite (in minuti). Restituisce null se non ci sono partite concluse.
     */
    default Double calculateAverageDuration(Long localeId) {
        java.util.List<PartitaCalciobalilla> partite = findConcluseByLocaleId(localeId);
        if (partite.isEmpty()) return null;
        return partite.stream()
                .mapToDouble(p -> java.time.Duration.between(p.getOrarioInizio(), p.getOrarioFine()).getSeconds() / 60.0)
                .average()
                .orElse(0.0);
    }

    @Query("SELECT p FROM PartitaCalciobalilla p WHERE p.localeId = :localeId AND (:stato = 'IN_CORSO' AND p.orarioFine IS NULL)")
    java.util.List<PartitaCalciobalilla> findByLocaleIdAndStato(@Param("localeId") Long localeId, @Param("stato") String stato);

    @Query("SELECT COUNT(p) FROM PartitaCalciobalilla p WHERE p.localeId = :localeId AND p.orarioInizio >= :today")
    long countToday(@Param("localeId") Long localeId, @Param("today") java.time.LocalDateTime today);
}