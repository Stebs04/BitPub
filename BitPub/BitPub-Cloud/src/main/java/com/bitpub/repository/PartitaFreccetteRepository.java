/**
 * Repository per la gestione della persistenza delle partite di Freccette.
 * * @author Timothy Giolito
 */
@Repository
public interface PartitaFreccetteRepository extends JpaRepository<PartitaFreccette, Long> {
    List<PartitaFreccette> findByLocaleIdAndStato(Long localeId, String stato);
    
    @Query("SELECT COUNT(p) FROM PartitaFreccette p WHERE p.localeId = :id AND p.dataInizio >= :today")
    long countToday(@Param("id") Long id, @Param("today") LocalDateTime today);

    @Query("SELECT AVG(FUNCTION('DATEDIFF', 'SECOND', p.dataInizio, p.dataFine)) / 60.0 " +
           "FROM PartitaFreccette p WHERE p.localeId = :id AND p.dataFine IS NOT NULL")
    Double calculateAverageDuration(@Param("id") Long id);
}