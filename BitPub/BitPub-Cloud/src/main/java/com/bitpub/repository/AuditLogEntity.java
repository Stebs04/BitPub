package com.bitpub.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entità JPA che rappresenta un record persistente nel log di audit del sistema.
 * Viene utilizzata per tracciare eventi critici, operazioni degli utenti e anomalie
 * all'interno dell'ecosistema BitPub per scopi di monitoraggio e sicurezza.
 *
 * @author Stefano Bellan 20054330
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    /** Identificativo univoco del record di audit (Chiave Primaria). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Marca temporale dell'occorrenza dell'evento. */
    private LocalDateTime timestamp;

    /** Livello di gravità dell'evento registrato (es. INFO, WARN, ERROR). */
    private String level;

    /** Origine della segnalazione, utile per distinguere tra Cloud e nodi Edge locali. */
    private String source;

    /** Descrizione testuale dettagliata dell'evento o dell'errore. */
    private String message;

    /** Specifica dell'operazione eseguita (es. "LOGIN", "STOP_SESSION", "TOGGLE_STATUS"). */
    private String action;

    /**
     * Costruttore predefinito.
     * Inizializza automaticamente il timestamp al momento della creazione dell'istanza.
     */
    public AuditLogEntity() {
        // Generazione automatica della data/ora corrente
        this.timestamp = LocalDateTime.now();
    }

    // --- Metodi Getter e Setter ---

    /** @return L'identificativo del log. */
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /** @return Il momento esatto dell'evento. */
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    /** @return La severità del messaggio. */
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    /** @return La sorgente del log. */
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    /** @return Il contenuto descrittivo del log. */
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    /** @return L'azione specifica tracciata. */
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
