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

    // Idempotency key. The cloud dedups on this so a replayed command is never applied twice.
    @Column(nullable = false, unique = true)
    private UUID originalEventId;

    @Column(nullable = false)
    private String gameInstanceId;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    // Absolute cloud URL this command replays to. Sensor events now go Edge -> Cloud over MQTT,
    // so only WebApp game-action / tournament-result commands are buffered here, each setting its
    // own URL (e.g. .../api/matches/{id}/action, .../api/v1/tournaments/.../result).
    @Column(nullable = false)
    private String targetEndpoint;

    // HTTP method for the replay. Sensor + action are POST; tournament result is PUT.
    @Column(nullable = false)
    @Builder.Default
    private String httpMethod = "POST";

    // Identity captured at submit time by validating the caller's JWT once. Replayed as
    // X-User-Id / X-User-Role so the cloud authorizes the command even if the original JWT
    // has since expired. Null for sensor events (no user actor).
    private String actorUserId;
    private String actorRole;

    @Column(nullable = false)
    private Instant createdAt;
}
