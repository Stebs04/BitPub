package com.bitpub.contracts.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = GoalEvent.class, name = "GOAL"),
        @JsonSubTypes.Type(value = MatchStartedEvent.class, name = "MATCH_STARTED"),
        @JsonSubTypes.Type(value = MatchEndedEvent.class, name = "MATCH_ENDED"),
        @JsonSubTypes.Type(value = ScoreEvent.class, name = "SCORE"),
        @JsonSubTypes.Type(value = EdgeHeartbeatEvent.class, name = "EDGE_HEARTBEAT")
})
public abstract class BaseSensorEvent {

    @NotNull
    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    private UUID correlationId; // Used to trace related events or for replay

    @NotNull
    @Builder.Default
    private Instant timestamp = Instant.now();

    @NotBlank
    private String source;

    @NotBlank
    private String gameId;

    @NotBlank
    private String localeId;

    @NotBlank
    @Builder.Default
    private String version = "1.0";
}
