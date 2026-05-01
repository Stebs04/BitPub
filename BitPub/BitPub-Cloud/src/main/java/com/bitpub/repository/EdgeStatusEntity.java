package com.bitpub.cloud.repository;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entità JPA per la persistenza dello stato operativo dei nodi Edge.
 * Monitora la connettività delle sedi locali all'interno del sistema BitPub,
 * fungendo da base per la visualizzazione dello stato di rete nella dashboard.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@Entity
@Table(name = "edge_status")
public class EdgeStatusEntity {

    /** Identificativo unico della sede, utilizzato come chiave primaria (PK). */
    @Id
    private String venueId;

    /** Stato di connettività corrente del nodo (es. "ONLINE", "OFFLINE"). */
    private String status;

    /** Timestamp relativo all'ultimo segnale di heartbeat ricevuto dalla sede. */
    private LocalDateTime lastSeen;

    /**
     * Costruttore predefinito richiesto dalle specifiche JPA per l'istanziazione via riflessione.
     */
    public EdgeStatusEntity() {}

    /**
     * Costruttore parametrizzato per la creazione o l'aggiornamento rapido dello stato.
     *
     * @param venueId L'identificativo univoco della sede locale.
     * @param status  Il nuovo stato da registrare.
     */
    public EdgeStatusEntity(String venueId, String status) {
        this.venueId = venueId;
        this.status = status;
        // Inizializzazione del timestamp all'istante di creazione/modifica
        this.lastSeen = LocalDateTime.now();
    }

    // --- Metodi Getter e Setter ---

    /** @return L'ID della sede monitorata. */
    public String getVenueId() { return venueId; }

    /** @param venueId L'ID della sede da impostare. */
    public void setVenueId(String venueId) { this.venueId = venueId; }

    /** @return Lo stato operativo memorizzato. */
    public String getStatus() { return status; }

    /** @param status Lo stato (ONLINE/OFFLINE) da aggiornare. */
    public void setStatus(String status) { this.status = status; }

    /** @return L'ultimo contatto registrato dal nodo. */
    public LocalDateTime getLastSeen() { return lastSeen; }

    /** @param lastSeen Il timestamp dell'ultimo segnale ricevuto. */
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
