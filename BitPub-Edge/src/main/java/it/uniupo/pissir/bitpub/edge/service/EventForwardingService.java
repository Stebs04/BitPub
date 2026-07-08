package it.uniupo.pissir.bitpub.edge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.edge.model.BufferedEvent;
import it.uniupo.pissir.bitpub.edge.repository.BufferedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
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
    private final MessageChannel cloudMqttOutboundChannel;
    private final ObjectMapper objectMapper;

    public EventForwardingService(RuleEngineService ruleEngineService, BufferedEventRepository repository,
                                  @Qualifier("cloudMqttOutboundChannel") MessageChannel cloudMqttOutboundChannel) {
        this.ruleEngineService = ruleEngineService;
        this.repository = repository;
        this.cloudMqttOutboundChannel = cloudMqttOutboundChannel;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Validated sensor events are forwarded Edge -> Cloud over MQTT (QoS1) instead of REST.
     * The broker durably queues them for match-service while it is down, so no app-level
     * buffering is needed here; match-service dedups on eventId, so a QoS1 redelivery is safe.
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<String> message) {
        String payload = message.getPayload();
        log.info("Received MQTT message on Edge: {}", payload);

        Optional<SensorEvent> optionalEvent = ruleEngineService.validateAndParse(payload);
        if (optionalEvent.isEmpty()) {
            return;
        }

        SensorEvent event = optionalEvent.get();

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize sensor event {}, dropping", event.getEventId(), e);
            return;
        }

        String topic = MqttTopics.getCloudSensorIngestTopic(event.getGameInstanceId());
        cloudMqttOutboundChannel.send(MessageBuilder.withPayload(json)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build());
        log.info("Event {} forwarded to Cloud via MQTT topic {}", event.getEventId(), topic);
    }

    /**
     * Replays a buffered WebApp command (interactive game action / tournament result) to the
     * cloud over REST. Kept for the {@link it.uniupo.pissir.bitpub.edge.controller.EdgeCommandController}
     * offline path, which needs synchronous response semantics (403/JWT relay) MQTT can't give.
     * Captured identity is replayed as X-User-Id / X-User-Role.
     * <p>
     * Return contract for the retry loop:
     *   true  = done, delete from buffer (2xx, or a 4xx that will never succeed = poison pill dropped);
     *   false = transient (cloud unreachable / 5xx) = keep buffered and retry later.
     */
    public boolean forwardCommand(String httpMethod, String targetEndpoint, String payloadJson,
                                  String actorUserId, String actorRole) {
        try {
            RestClient.RequestBodySpec spec = RestClient.create().method(HttpMethod.valueOf(httpMethod))
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
