package com.bitpub.repository;

import com.bitpub.models.PartitaCalciobalilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Interfaccia di persistenza per l'entità {@link PartitaCalciobalilla}.
 * Estende {@link JpaRepository} per fornire le operazioni CRUD standard e definisce
 * query personalizzate (JPQL) per il calcolo delle statistiche globali.
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
}
