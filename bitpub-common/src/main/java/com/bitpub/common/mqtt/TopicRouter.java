package com.bitpub.common.mqtt;

import com.bitpub.contracts.events.BaseSensorEvent;
import com.fasterxml.jackson.annotation.JsonSubTypes;

import java.util.HashMap;
import java.util.Map;

public class TopicRouter {

    private static final String TOPIC_TEMPLATE = "bitpub/locales/%s/games/%s/events/%s";
    private static final Map<Class<? extends BaseSensorEvent>, String> EVENT_TYPE_MAP = new HashMap<>();

    static {
        // Dynamically resolve event types from the @JsonSubTypes annotation on BaseSensorEvent
        JsonSubTypes subTypes = BaseSensorEvent.class.getAnnotation(JsonSubTypes.class);
        if (subTypes != null) {
            for (JsonSubTypes.Type type : subTypes.value()) {
                @SuppressWarnings("unchecked")
                Class<? extends BaseSensorEvent> eventClass = (Class<? extends BaseSensorEvent>) type.value();
                EVENT_TYPE_MAP.put(eventClass, type.name());
            }
        }
    }

    /**
     * Determines the MQTT topic for a given event based on its properties and class.
     *
     * @param event The event to route.
     * @return The formatted MQTT topic string.
     * @throws IllegalArgumentException if the event class is not mapped to an event type.
     */
    public static String getTopicFor(BaseSensorEvent event) {
        if (event == null || event.getLocaleId() == null || event.getGameId() == null) {
            throw new IllegalArgumentException("Event, localeId, and gameId must not be null");
        }

        String eventType = EVENT_TYPE_MAP.get(event.getClass());
        if (eventType == null) {
            throw new IllegalArgumentException("Unknown event type for class: " + event.getClass().getName());
        }

        return String.format(TOPIC_TEMPLATE, event.getLocaleId(), event.getGameId(), eventType);
    }
    
    /**
     * Extracts the routing context (locale, game, eventType) from a topic.
     * Useful for consumers to understand context outside of the payload.
     *
     * @param topic The MQTT topic string.
     * @return An array containing [localeId, gameId, eventType] or null if the topic format is invalid.
     */
    public static String[] extractContextFromTopic(String topic) {
        if (topic == null) {
            return null;
        }
        String[] parts = topic.split("/");
        if (parts.length >= 6 && "bitpub".equals(parts[0]) && "locales".equals(parts[1]) && "games".equals(parts[3]) && "events".equals(parts[5])) {
            return new String[]{parts[2], parts[4], parts[6]};
        }
        return null;
    }
}
