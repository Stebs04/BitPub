package com.bitpub.cloud.repository; 

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entità JPA configurata per la persistenza dello stato operativo dei nodi Edge.
 * Gestisce il tracciamento della connettività delle sedi nel database PostgreSQL,
 * fornendo i dati necessari per il monitoraggio della rete in tempo reale.
 * 
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@Entity
@Table(name = "edge_status")
public class EdgeStatusEntity {

    /** Identificativo univoco della sede (Venue), utilizzato come chiave primaria. */
    @Id
    private String venueId;

    /** Nome descrittivo assegnato alla sede fisica. */
    private String venueName;

    /** Stato corrente della connettività (es. ONLINE, OFFLINE). */
    private String status;

    /** Marca temporale dell'ultima attività registrata (Heartbeat). */
    private LocalDateTime lastSeen; 

    /**
     * Costruttore predefinito richiesto dalle specifiche JPA per l'istanziazione tramite reflection.
     */
    public EdgeStatusEntity() {
        // Inizializzazione protetta per il framework di persistenza
    }

    /**
     * Costruttore specializzato per l'aggiornamento rapido dello stato tramite gateway MQTT.
     * Inizializza automaticamente la marca temporale all'istante corrente.
     * 
     * @param venueId L'identificativo univoco della sede locale.
     * @param status  Il nuovo stato di connettività da registrare.
     */
    public EdgeStatusEntity(String venueId, String status) {
        this.venueId = venueId;
        this.status = status;
        // Generazione automatica del timestamp per il monitoraggio Last Seen
        this.lastSeen = LocalDateTime.now(); 
    }

    // --- Metodi Getter e Setter per l'accesso ai dati ---

    /** @return L'identificativo della sede monitorata. */
    public String getVenueId() { return venueId; }
    
    /** @param venueId L'ID della sede da impostare. */
    public void setVenueId(String venueId) { this.venueId = venueId; }

    /** @return Il nome della sede. */
    public String getVenueName() { return venueName; }
    
    /** @param venueName Il nome descrittivo da assegnare. */
    public void setVenueName(String venueName) { this.venueName = venueName; }

    /** @return Lo stato operativo memorizzato. */
    public String getStatus() { return status; }
    
    /** @param status Lo stato (ONLINE/OFFLINE) da aggiornare. */
    public void setStatus(String status) { this.status = status; }

    /** @return L'ultimo timestamp di attività registrato dal nodo. */
    public LocalDateTime getLastSeen() { return lastSeen; }
    
    /** @param lastSeen La marca temporale dell'ultimo segnale ricevuto. */
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
