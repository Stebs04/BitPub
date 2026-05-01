package com.bitpub.controllers;

import com.bitpub.repository.AuditLogEntity;
import com.bitpub.repository.AuditLogRepository;
import com.bitpub.cloud.repository.EdgeStatusEntity;
import com.bitpub.repository.EdgeStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST per la gestione e l'esposizione dei log di sistema e audit trail.
 * Supporta il filtraggio dinamico e aderisce alle specifiche di Semantic Versioning
 * tramite la gestione dei Media Type personalizzati negli header.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemLogController {

    /** Accesso allo strato di persistenza per i dati di audit. */
    @Autowired
    private AuditLogRepository logRepository;

    /** Accesso allo strato di persistenza per lo stato dei nodi Edge. */
    @Autowired
    private EdgeStatusRepository edgeStatusRepository;

    /**
     * Recupera l'elenco dei log di sistema, con possibilità di filtraggio per severità.
     * Utilizza il Media Type v1 per garantire la compatibilità con i client della stessa versione.
     *
     * Endpoint: GET /api/v1/system/logs
     * Query Params: level (opzionale, es: INFO, WARN, ERROR)
     *
     * @param level Il livello di severità desiderato per filtrare i risultati.
     * @return Una {@link List} di {@link AuditLogEntity} filtrata o completa.
     */
    @GetMapping(value = "/logs", produces = "application/resources.v1+json")
    public List<AuditLogEntity> getLogs(@RequestParam(required = false) String level) {

        // Verifica la presenza di un filtro valido e diverso dal valore jolly "ALL"
        if (level != null && !level.isEmpty() && !"ALL".equals(level)) {
            // Esecuzione della query filtrata tramite metodo derivato nel Repository
            return logRepository.findByLevel(level);
        }

        // Restituzione di tutti i log presenti nel database se nessun filtro è applicato
        return logRepository.findAll();
    }

    /**
     * Recupera l'elenco dello stato attuale di connessione dei nodi edge.
     * Endpoint: GET /api/v1/system/network-status
     *
     * @return Una {@link List} di {@link EdgeStatusEntity} con le info sui vari locali.
     */
    @GetMapping(value = "/network-status", produces = "application/resources.v1+json")
    public List<EdgeStatusEntity> getNetworkStatus() {
        return edgeStatusRepository.findAll();
    }
}
