package com.bitpub.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Rappresenta un'entrata di log per i messaggi che transitano sul broker MQTT.
 * <p>
 * Questa entità viene utilizzata per la persistenza su database PostgreSQL,
 * permettendo di mantenere uno storico di tutte le comunicazioni tra i dispositivi
 * Edge e il Cloud.
 * </p>
 * @author Timothy Giolito 20054431
 */
@Entity
@Table(name = "mqtt_logs") // Il nome della tabella nel database PostgreSQL
public class MqttLog {

    /**
     * Identificativo univoco del log, generato automaticamente dal database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Il canale MQTT sul quale è stato trasmesso il messaggio (es. "locali/01/freccette/score").
     */
    private String topic;

    /**
     * Il contenuto effettivo del messaggio.
     * <p>
     * Viene definito come {@code TEXT} nel database per permettere il salvataggio
     * di stringhe JSON complesse e potenzialmente molto estese.
     * </p>
     */
    @Column(columnDefinition = "TEXT") // Per messaggi JSON lunghi
    private String payload;

    /**
     * Data e ora della creazione del log all'interno del sistema.
     */
    private LocalDateTime timestamp;

    /**
     * Costruttore predefinito.
     * Necessario per il framework JPA per istanziare l'oggetto durante il recupero dal database.
     */
    public MqttLog() {}

    /**
     * Costruttore per la creazione rapida di un nuovo log.
     * Imposta automaticamente il timestamp al momento corrente.
     *
     * @param topic Il canale di provenienza del messaggio.
     * @param payload Il contenuto del messaggio ricevuto.
     */
    public MqttLog(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    // --- GETTER E SETTER ---
    // Essenziali per Spring Data JPA e per le operazioni di serializzazione.

    /** @return L'ID univoco del log. */
    public Long getId() { return id; }
    /** @return Il topic MQTT associato al log. */
    public String getTopic() { return topic; }
    /** @return Il contenuto (JSON) del messaggio. */
    public String getPayload() { return payload; }
    /** @return L'istante di registrazione del log. */
    public LocalDateTime getTimestamp() { return timestamp; }
}