package it.uniupo.pissir.bitpub.edge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.edge.model.BufferedEvent;
import it.uniupo.pissir.bitpub.edge.repository.BufferedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class EventForwardingService {

    private final RuleEngineService ruleEngineService;
    private final BufferedEventRepository repository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public EventForwardingService(RuleEngineService ruleEngineService, BufferedEventRepository repository, RestClient restClient) {
        this.ruleEngineService = ruleEngineService;
        this.repository = repository;
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<String> message) {
        String payload = message.getPayload();
        log.info("Received MQTT message on Edge: {}", payload);

        Optional<SensorEvent> optionalEvent = ruleEngineService.validateAndParse(payload);
        if (optionalEvent.isEmpty()) {
            return;
        }

        SensorEvent event = optionalEvent.get();

        // Check if we already buffered this to avoid duplicate processing on local redeliveries
        if (repository.existsByOriginalEventId(event.getEventId())) {
            log.info("Event {} already buffered, skipping.", event.getEventId());
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize sensor event {}, dropping", event.getEventId(), e);
            return;
        }

        // Sensor events are POSTed to the match-service ingest path (relative to the base RestClient).
        boolean success = forwardCommand("POST", "/api/matches/events", json, null, null);
        if (!success) {
            log.warn("Cloud unreachable or error, buffering event {}", event.getEventId());
            bufferCommand(event.getEventId(), event.getGameInstanceId(), "POST", "/api/matches/events", json, null, null);
        } else {
            log.info("Event {} successfully forwarded to Cloud.", event.getEventId());
        }
    }

    /**
     * Sends a command to the cloud. A relative targetEndpoint uses the match-service base
     * RestClient; an absolute (http...) one is sent as-is, letting a single mechanism replay
     * to any cloud service (e.g. tournament-service results). Captured identity is replayed
     * as X-User-Id / X-User-Role (null for sensor events).
     * <p>
     * Return contract for the retry loop:
     *   true  = done, delete from buffer (2xx, or a 4xx that will never succeed = poison pill dropped);
     *   false = transient (cloud unreachable / 5xx) = keep buffered and retry later.
     */
    public boolean forwardCommand(String httpMethod, String targetEndpoint, String payloadJson,
                                  String actorUserId, String actorRole) {
        try {
            RestClient client = targetEndpoint.startsWith("http") ? RestClient.create() : restClient;
            RestClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(httpMethod))
                    .uri(targetEndpoint)
                    .contentType(MediaType.APPLICATION_JSON);
            if (actorUserId != null) spec = spec.header("X-User-Id", actorUserId);
            if (actorRole != null) spec = spec.header("X-User-Role", actorRole);
            if (payloadJson != null && !payloadJson.isBlank()) {
                spec.body(payloadJson).retrieve().toBodilessEntity();
            } else {
                spec.retrieve().toBodilessEntity();
            }
            return true;
        } catch (ResourceAccessException e) {
            // Cloud unreachable (connection refused / timeout) — the offline case. Retry later.
            log.warn("Cloud unreachable for {} {}: {}", httpMethod, targetEndpoint, e.getMessage());
            return false;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is5xxServerError()) {
                log.warn("Cloud 5xx for {} {}: retrying later", httpMethod, targetEndpoint);
                return false;
            }
            // 4xx: a genuine rejection (e.g. 403 not-your-turn). Retrying never helps; drop it.
            log.error("Cloud rejected {} {} with {} — dropping command: {}",
                    httpMethod, targetEndpoint, e.getStatusCode(), e.getResponseBodyAsString());
            return true;
        }
    }

    /** Convenience overload for the scheduler replaying a stored command. */
    public boolean forwardCommand(BufferedEvent cmd) {
        return forwardCommand(cmd.getHttpMethod(), cmd.getTargetEndpoint(), cmd.getPayloadJson(),
                cmd.getActorUserId(), cmd.getActorRole());
    }

    /**
     * Persists a command for deferred retry. Idempotent on the originalEventId so a local
     * redelivery never double-buffers. gameInstanceId is a sensor-context tag; non-sensor
     * commands pass a sentinel.
     */
    @Transactional
    public void bufferCommand(UUID eventId, String gameInstanceId, String httpMethod, String targetEndpoint,
                              String payloadJson, String actorUserId, String actorRole) {
        if (repository.existsByOriginalEventId(eventId)) {
            return;
        }
        repository.save(BufferedEvent.builder()
                .originalEventId(eventId)
                .gameInstanceId(gameInstanceId != null ? gameInstanceId : "-")
                .payloadJson(payloadJson)
                .httpMethod(httpMethod)
                .targetEndpoint(targetEndpoint)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .createdAt(Instant.now())
                .build());
    }
}
