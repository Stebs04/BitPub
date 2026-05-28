package com.bitpub.common.mqtt;

import com.bitpub.contracts.events.BaseSensorEvent;

public interface EventConsumer {

    /**
     * Consumes an event received from an MQTT topic.
     * Implementations should handle routing based on the event's actual class (GoalEvent, ScoreEvent, etc.)
     * and handle validation.
     *
     * @param topic The MQTT topic the event was received on.
     * @param event The deserialized event.
     */
    void consume(String topic, BaseSensorEvent event);
}
