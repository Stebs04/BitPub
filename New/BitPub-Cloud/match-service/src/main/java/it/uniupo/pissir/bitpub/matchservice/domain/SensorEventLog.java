package it.uniupo.pissir.bitpub.matchservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sensor_event_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true) // L'ID dell'evento generato dall'edge, garantisce l'idempotenza
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @Column(nullable = false)
    private String sensorType; // Es. GOAL, DART_HIT, BALL_POCKETED

    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false)
    private Instant receivedAt; // Timestamp di quando è stato ingerito dal cloud

    @Column(columnDefinition = "TEXT")
    private String payload; // Dati addizionali dell'evento in formato JSON
}
