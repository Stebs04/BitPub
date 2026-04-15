package com.bitpub.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mqtt_logs") // Il nome della tabella nel database PostgreSQL
public class MqttLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;

    @Column(columnDefinition = "TEXT") // Per messaggi JSON lunghi
    private String payload;

    private LocalDateTime timestamp;

    // Costruttore vuoto obbligatorio per JPA
    public MqttLog() {}

    public MqttLog(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
    }

    // Getter e Setter (fondamentali per Spring)
    public Long getId() { return id; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public LocalDateTime getTimestamp() { return timestamp; }
}