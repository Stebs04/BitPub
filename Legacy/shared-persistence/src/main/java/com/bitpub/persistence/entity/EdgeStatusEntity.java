package com.bitpub.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * EdgeStatusEntity - Monitoraggio dello stato di connettività dei nodi locali.
 * * Refactoring Senior Note:
 * Questa entità riceve aggiornamenti frequenti (heartbeat). L'uso del locking
 * ottimistico evita deadlock nel database PostgreSQL durante picchi di traffico MQTT.
 */
@Entity
@Table(name = "edge_status")
public class EdgeStatusEntity {

    @Id
    private String venueId;

    private String status; // ONLINE, OFFLINE

    private LocalDateTime lastSeen;

    @Version
    private Long version;

    public String getVenueId() { return venueId; }
    public void setVenueId(String venueId) { this.venueId = venueId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

