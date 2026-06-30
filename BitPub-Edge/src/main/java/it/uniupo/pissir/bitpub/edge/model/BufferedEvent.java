package it.uniupo.pissir.bitpub.edge.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "buffered_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BufferedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID originalEventId;

    @Column(nullable = false)
    private String gameInstanceId;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Column(nullable = false)
    private Instant createdAt;
}
