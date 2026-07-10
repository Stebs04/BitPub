// Autore: Timothy Giolito 20054431
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

    @Column(nullable = false, unique = true) // Identificativo generato dall'edge per l'evento, necessario per garantire idempotenza
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @Column(nullable = false)
    private String sensorType; // Categoria del sensore (es. GOAL, DART_HIT, BALL_POCKETED)

    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false)
    private Instant receivedAt; // Timestamp di ingestione del record da parte del servizio cloud

    @Column(columnDefinition = "TEXT")
    private String payload; // Informazioni aggiuntive in formato JSON inerenti all'evento
}
