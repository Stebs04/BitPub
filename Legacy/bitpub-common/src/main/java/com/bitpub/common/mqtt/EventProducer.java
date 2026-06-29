package com.bitpub.common.mqtt;

import com.bitpub.contracts.events.BaseSensorEvent;

public interface EventProducer {

    /**
     * Publishes an event to the appropriate MQTT topic.
     * The topic should be determined by the event type and its properties (e.g., gameId, localeId).
     *
     * @param event The event to publish.
     */
    void publish(BaseSensorEvent event);
}
