package com.bitpub.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * AuditLogEntity - Registro delle operazioni di sistema e sicurezza.
 * * Refactoring Senior Note:
 * Sebbene l'audit sia prevalentemente inserimento, l'aggiunta della versione 
 * mantiene la coerenza con il resto del modello di dominio per eventuali 
 * riconciliazioni o modifiche amministrative post-registrazione.
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

    private String message;

    private LocalDateTime timestamp;

    @Version
    private Long version;

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