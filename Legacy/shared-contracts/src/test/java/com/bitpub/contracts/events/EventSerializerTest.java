package com.bitpub.contracts.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EventSerializerTest {

    @Test
    public void testSerializeAndDeserializeGoalEvent() throws JsonProcessingException {
        GoalEvent original = GoalEvent.builder()
                .eventId(UUID.randomUUID())
                .source("sensor-1")
                .gameId("game-123")
                .localeId("locale-abc")
                .team("Red")
                .player("John Doe")
                .isOwnGoal(false)
                .build();

        String json = EventSerializer.serialize(original);
        assertNotNull(json);
        assertTrue(json.contains("\"eventType\":\"GOAL\""));

        BaseSensorEvent deserialized = EventSerializer.deserialize(json);
        assertTrue(deserialized instanceof GoalEvent);
        
        GoalEvent goalEvent = (GoalEvent) deserialized;
        assertEquals(original.getEventId(), goalEvent.getEventId());
        assertEquals("Red", goalEvent.getTeam());
        assertEquals("John Doe", goalEvent.getPlayer());
        assertFalse(goalEvent.isOwnGoal());
    }

    @Test
    public void testSerializeAndDeserializeMatchStartedEvent() throws JsonProcessingException {
        MatchStartedEvent original = MatchStartedEvent.builder()
                .eventId(UUID.randomUUID())
                .source("sensor-2")
                .gameId("game-456")
                .localeId("locale-xyz")
                .matchMode("2v2")
                .players(List.of("P1", "P2", "P3", "P4"))
                .build();

        String json = EventSerializer.serialize(original);
        assertNotNull(json);
        assertTrue(json.contains("\"eventType\":\"MATCH_STARTED\""));

        BaseSensorEvent deserialized = EventSerializer.deserialize(json);
        assertTrue(deserialized instanceof MatchStartedEvent);

        MatchStartedEvent matchEvent = (MatchStartedEvent) deserialized;
        assertEquals("2v2", matchEvent.getMatchMode());
        assertEquals(4, matchEvent.getPlayers().size());
    }
}
