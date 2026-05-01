package com.bitpub.models;

import com.google.gson.annotations.Expose;

/**
 * Modello che rappresenta lo stato di connessione di un nodo Edge (Locale).
 * Fornisce informazioni in tempo reale sulla disponibilità delle sedi fisiche.
 *
 * @author Stefano Bellan 20054330
 */
public class EdgeStatus {

    /** Identificativo univoco della sede (Venue) */
    @Expose private String venueId;

    /** Nome descrittivo della sede locale */
    @Expose private String venueName;

    /** Stato operativo attuale della connessione (es. ONLINE o OFFLINE) */
    @Expose private String status;

    /** Marca temporale dell'ultima comunicazione ricevuta dal nodo (Heartbeat) */
    @Expose private String lastSeen;

    /**
     * Costruttore predefinito.
     * Utilizzato dalla libreria GSON per l'istanziazione tramite reflection.
     */
    public EdgeStatus() {
        // Costruttore vuoto per deserializzazione JSON
    }

    /** @return L'identificativo della sede */
    public String getVenueId() { return venueId; }

    /** @return Il nome della sede */
    public String getVenueName() { return venueName; }

    /** @return Lo stato attuale (ONLINE/OFFLINE) */
    public String getStatus() { return status; }

    /** @return Il timestamp dell'ultimo ping ricevuto */
    public String getLastSeen() { return lastSeen; }
}
