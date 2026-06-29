package com.bitpub.common.mqtt;

import com.bitpub.contracts.events.BaseSensorEvent;
import com.bitpub.contracts.events.EventSerializer;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class MqttEventDispatcher implements IMqttMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MqttEventDispatcher.class);

    private final List<EventConsumer> consumers;

    @Autowired
    public MqttEventDispatcher(List<EventConsumer> consumers) {
        this.consumers = consumers;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            
            // 1. Deserialize (schema evolution supported via Jackson config)
            BaseSensorEvent event = EventSerializer.deserialize(payload);

            // 2. Validate
            EventValidator.validate(event);

            // 3. Dispatch to all consumers
            for (EventConsumer consumer : consumers) {
                try {
                    consumer.consume(topic, event);
                } catch (Exception e) {
                    logger.error("Consumer {} failed to process event {} from topic {}", 
                                 consumer.getClass().getSimpleName(), event.getEventId(), topic, e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process incoming MQTT message on topic {}", topic, e);
            // Non rilanciamo l'eccezione per non bloccare il client MQTT.
            // In un sistema di produzione, gli eventi "poison pill" andrebbero salvati in una DLQ (Dead Letter Queue).
        }
    }
}
