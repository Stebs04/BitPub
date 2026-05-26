package com.bitpub.common.mqtt;

import com.bitpub.contracts.events.GoalEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventValidatorTest {

    @Test
    void testValidate_ValidEvent() {
        GoalEvent event = GoalEvent.builder()
                .eventId(UUID.randomUUID())
                .source("sensor-1")
                .localeId("loc-1")
                .gameId("game-1")
                .team("red")
                .version("1.0")
                .build();

        assertDoesNotThrow(() -> EventValidator.validate(event));
    }

    @Test
    void testValidate_MissingRequiredField() {
        GoalEvent event = GoalEvent.builder()
                .eventId(UUID.randomUUID())
                .source("sensor-1")
                // Missing localeId, gameId, team
                .build();

        assertThrows(IllegalArgumentException.class, () -> EventValidator.validate(event));
    }
}
