package com.bitpub.repository;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * EdgeStatusEntity - Monitoraggio dello stato di connettività dei nodi locali.
 * * Refactoring Senior Note:
 * Questa entità riceve aggiornamenti frequenti (heartbeat). L'uso del locking
 * ottimistico evita deadlock nel database PostgreSQL durante picchi di traffico MQTT.
 */
@Entity
@Table(name = "edge_status")
@Data
public class EdgeStatusEntity {

    @Id
    private String venueId;

    private String status; // ONLINE, OFFLINE

    private LocalDateTime lastSeen;

    @Version
    private Long version;
}