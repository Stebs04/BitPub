package com.bitpub.models;

import com.google.gson.annotations.Expose;

/**
 * Rappresenta una sessione di gioco attiva all'interno di una sede (Biliardo, Calciobalilla, Freccette).
 * Estende {@link ResourceModel} per l'integrazione con le logiche di risorsa del sistema BitPub.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
public class GameSession extends ResourceModel {

    /** Identificativo univoco della sessione di gioco */
    @Expose private String sessionId;

    /** Identificativo della sede in cui si svolge la sessione */
    @Expose private String venueId;

    /** Identificativo del tavolo o della postazione fisica utilizzata */
    @Expose private String tableId;

    /** Marca temporale di inizio della sessione */
    @Expose private String startTime;

    /** Stato operativo attuale: STARTING (In avvio), IN_PROGRESS (Attiva), PAUSED (In pausa) */
    @Expose private String status;

    /**
     * Costruttore predefinito per la libreria GSON.
     * Necessario per la deserializzazione dei dati provenienti dalle API Cloud.
     */
    public GameSession() {
        // Costruttore vuoto per riflessione
    }

    /** @return L'ID univoco della sessione */
    public String getSessionId() { return sessionId; }

    /** @return L'ID della sede di riferimento */
    public String getVenueId() { return venueId; }

    /** @return L'ID del tavolo o risorsa fisica */
    public String getTableId() { return tableId; }

    /** @return Il timestamp di inizio sessione */
    public String getStartTime() { return startTime; }

    /** @return Lo stato attuale della sessione di gioco */
    public String getStatus() { return status; }
}
