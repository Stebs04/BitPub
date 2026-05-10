package com.bitpub.repository;

import com.bitpub.models.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Interfaccia di persistenza per l'entità AuditLogEntity.
 * Estende JpaRepository per fornire le operazioni CRUD standard e metodi di ricerca
 * personalizzati tramite Spring Data JPA.
 *
 * @author Stefano Bellan 20054330
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * Recupera una lista di log filtrati in base al livello di severità.
     * Questo metodo supporta la logica di filtraggio richiesta dalle specifiche
     * della dashboard amministrativa.
     *
     * @param level Il livello di severità da cercare (es. "INFO", "WARN", "ERROR").
     * @return Una {@link List} di entità corrispondenti al criterio.
     */
    List<AuditLogEntity> findByLevel(String level);
}