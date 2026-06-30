package it.uniupo.pissir.bitpub.edge.service;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RuleEngineServiceTest {

    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        ruleEngineService = new RuleEngineService();
    }

    @Test
    void validateAndParse_ValidPayload_ReturnsEvent() {
        String payload = "{\"eventId\":\"evt123\",\"gameInstanceId\":\"game1\",\"sensorType\":\"INFRARED\",\"value\":\"GOAL\",\"timestamp\":\"2026-06-29T10:00:00Z\"}";
        
        Optional<SensorEvent> result = ruleEngineService.validateAndParse(payload);
        
        assertTrue(result.isPresent());
        assertEquals("evt123", result.get().getEventId());
        assertEquals("game1", result.get().getGameInstanceId());
        assertEquals("INFRARED", result.get().getSensorType());
    }

    @Test
    void validateAndParse_MissingRequiredFields_ReturnsEmpty() {
        String payload = "{\"sensorType\":\"INFRARED\",\"value\":\"GOAL\"}"; // missing eventId and gameInstanceId
        
        Optional<SensorEvent> result = ruleEngineService.validateAndParse(payload);
        
        assertFalse(result.isPresent());
    }

    @Test
    void validateAndParse_InvalidJson_ReturnsEmpty() {
        String payload = "{ invalid_json }";
        
        Optional<SensorEvent> result = ruleEngineService.validateAndParse(payload);
        
        assertFalse(result.isPresent());
    }
}
