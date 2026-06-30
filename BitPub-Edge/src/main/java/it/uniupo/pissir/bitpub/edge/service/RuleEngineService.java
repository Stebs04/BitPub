package it.uniupo.pissir.bitpub.edge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class RuleEngineService {

    private final ObjectMapper objectMapper;

    public RuleEngineService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public Optional<SensorEvent> validateAndParse(String payload) {
        try {
            SensorEvent event = objectMapper.readValue(payload, SensorEvent.class);
            if (event.getEventId() == null || event.getGameInstanceId() == null || event.getSensorType() == null) {
                log.warn("Invalid SensorEvent: missing required fields. Payload: {}", payload);
                return Optional.empty();
            }
            return Optional.of(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse SensorEvent from payload: {}", payload, e);
            return Optional.empty();
        }
    }
}
