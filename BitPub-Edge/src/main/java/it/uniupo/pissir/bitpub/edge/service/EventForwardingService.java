package it.uniupo.pissir.bitpub.edge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.edge.model.BufferedEvent;
import it.uniupo.pissir.bitpub.edge.repository.BufferedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class EventForwardingService {

    private final RuleEngineService ruleEngineService;
    private final BufferedEventRepository repository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${bitpub.edge.locale-id}")
    private String localeId;

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

        boolean success = forwardToCloud(event);
        if (!success) {
            log.warn("Cloud unreachable or error, buffering event {}", event.getEventId());
            bufferEvent(event);
        } else {
            log.info("Event {} successfully forwarded to Cloud.", event.getEventId());
        }
    }

    public boolean forwardToCloud(SensorEvent event) {
        try {
            restClient.post()
                    .uri("/api/matches/events") // POST to /api/matches/events on match-service
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Failed to forward event to cloud: {}", e.getMessage());
            return false;
        }
    }

    private void bufferEvent(SensorEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            BufferedEvent bufferedEvent = BufferedEvent.builder()
                    .originalEventId(event.getEventId())
                    .gameInstanceId(event.getGameInstanceId())
                    .payloadJson(json)
                    .createdAt(Instant.now())
                    .build();
            repository.save(bufferedEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for buffering", e);
        }
    }
}
