package com.bitpub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Interfaccia di persistenza per l'entità {@link AuditLogEntity}.
 * Estende JpaRepository per fornire le operazioni CRUD standard e metodi di ricerca
 * personalizzati tramite Spring Data JPA.
 *
 * @author Stefano Bellan 20054330
 */
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    /**
     * Recupera una lista di log filtrati in base al livello di severità.
     * Questo metodo supporta la logica di filtraggio richiesta dalle specifiche
     * della dashboard amministrativa.
     *
     * @param level Il livello di severità da cercare (es. "INFO", "WARN", "ERROR").
     * @return Una {@link List} di {@link AuditLogEntity} corrispondenti al criterio.
     */
    List<AuditLogEntity> findByLevel(String level);
}
