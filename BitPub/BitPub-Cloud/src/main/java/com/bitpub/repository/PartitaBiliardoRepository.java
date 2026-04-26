/**
 * Repository per la gestione della persistenza delle partite di Biliardo.
 * @author Luca Franzon
 */
@Repository
public interface PartitaBiliardoRepository extends JpaRepository<PartitaBiliardo, Long> {
    List<PartitaBiliardo> findByLocaleIdAndStato(Long localeId, String stato);
    
    @Query("SELECT COUNT(p) FROM PartitaBiliardo p WHERE p.localeId = :id AND p.dataInizio >= :today")
    long countToday(@Param("id") Long id, @Param("today") LocalDateTime today);

    @Query("SELECT AVG(FUNCTION('DATEDIFF', 'SECOND', p.dataInizio, p.dataFine)) / 60.0 " +
           "FROM PartitaBiliardo p WHERE p.localeId = :id AND p.dataFine IS NOT NULL")
    Double calculateAverageDuration(@Param("id") Long id);
}
