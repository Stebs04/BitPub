package com.bitpub.repository;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AuditLogEntity - Registro delle operazioni di sistema e sicurezza.
 * * Refactoring Senior Note:
 * Sebbene l'audit sia prevalentemente inserimento, l'aggiunta della versione 
 * mantiene la coerenza con il resto del modello di dominio per eventuali 
 * riconciliazioni o modifiche amministrative post-registrazione.
 */
@Entity
@Table(name = "audit_logs")
@Data
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String action;

    private String details;

    private LocalDateTime timestamp;

    @Version
    private Long version;
}