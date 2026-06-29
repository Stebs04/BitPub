package com.bitpub.persistence.entity;

import com.google.gson.annotations.Expose;

/**
 * Rappresenta un evento di log di sistema o di audit all'interno dell'ecosistema BitPub.
 * Questa classe POJO è utilizzata per mappare i dati provenienti dai servizi di monitoring
 * e visualizzarli nelle dashboard di amministrazione.
 *
 * @author Stefano Bellan 20054330
 */
public class SystemLog {

    /** Marca temporale dell'evento (formato ISO o leggibile) */
    @Expose
    private String timestamp;

    /** Livello di severità dell'evento: INFO, WARN, ERROR */
    @Expose
    private String level;

    /** Origine della segnalazione: CLOUD o identificativo dell'istanza locale */
    @Expose
    private String source;

    /** Descrizione testuale dettagliata dell'evento */
    @Expose
    private String message;

    /** Tipologia di operazione eseguita per l'audit trail (es. "LOGIN", "STOP_SESSION") */
    @Expose
    private String action;

    /**
     * Costruttore predefinito senza argomenti.
     * Necessario per la corretta deserializzazione da parte della libreria GSON.
     */
    public SystemLog() {
        // Costruttore vuoto per riflessione GSON
    }

    /**
     * @return Il timestamp registrato per l'evento.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @return Il livello di criticità del log.
     */
    public String getLevel() {
        return level;
    }

    /**
     * @return La sorgente che ha generato l'evento.
     */
    public String getSource() {
        return source;
    }

    /**
     * @return Il contenuto del messaggio di log.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return L'azione specifica tracciata nell'audit trail.
     */
    public String getAction() {
        return action;
    }
}


