package com.bitpub.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Column;

/**
 * AuditLogEntity - Registro delle operazioni di sistema e sicurezza.
 * * Refactoring Senior Note:
 * È stato aggiunto un metodo @PrePersist per garantire che ogni log abbia 
 * un timestamp di sistema valido senza delegare la responsabilità al chiamante.
 * La colonna timestamp è marcata come non nullable per integrità dei dati.
 * * @author Stefano Bellan 20054330
 */
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;

    private String action;

    private String level;

    @Column(length = 1000)
    private String message;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Version
    private Long version;

    /**
     * Ciclo di vita JPA: Assicura che la data di registrazione sia sempre presente.
     */
    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}