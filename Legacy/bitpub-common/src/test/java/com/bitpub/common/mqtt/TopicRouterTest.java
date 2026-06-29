package com.bitpub.common.mqtt;

import com.bitpub.contracts.events.GoalEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TopicRouterTest {

    @Test
    void testGetTopicFor_ValidEvent() {
        GoalEvent event = GoalEvent.builder()
                .eventId(UUID.randomUUID())
                .source("sensor")
                .localeId("loc-1")
                .gameId("game-1")
                .team("red")
                .build();

        String topic = TopicRouter.getTopicFor(event);
        assertThat(topic).isEqualTo("bitpub/locales/loc-1/games/game-1/events/GOAL");
    }

    @Test
    void testExtractContextFromTopic_ValidTopic() {
        String topic = "bitpub/locales/loc-1/games/game-1/events/GOAL";
        String[] context = TopicRouter.extractContextFromTopic(topic);
        
        assertThat(context).isNotNull();
        assertThat(context[0]).isEqualTo("loc-1");
        assertThat(context[1]).isEqualTo("game-1");
        assertThat(context[2]).isEqualTo("GOAL");
    }

    @Test
    void testGetTopicFor_MissingFields() {
        GoalEvent event = GoalEvent.builder()
                .eventId(UUID.randomUUID())
                .source("sensor")
                // Missing localeId and gameId
                .build();

        assertThrows(IllegalArgumentException.class, () -> TopicRouter.getTopicFor(event));
    }
}
