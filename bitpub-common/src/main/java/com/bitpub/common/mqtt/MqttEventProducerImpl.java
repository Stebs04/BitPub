package com.bitpub.common.mqtt;

import com.bitpub.contracts.events.BaseSensorEvent;
import com.bitpub.contracts.events.EventSerializer;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttEventProducerImpl implements EventProducer {

    private static final Logger logger = LoggerFactory.getLogger(MqttEventProducerImpl.class);

    private final IMqttClient mqttClient;

    @Autowired
    public MqttEventProducerImpl(IMqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    @Override
    public void publish(BaseSensorEvent event) {
        try {
            // 1. Validate the event
            EventValidator.validate(event);

            // 2. Determine the topic
            String topic = TopicRouter.getTopicFor(event);

            // 3. Serialize the event
            String payload = EventSerializer.serialize(event);

            // 4. Publish
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1); // At least once delivery is standard for rigorous models
            message.setRetained(false);

            mqttClient.publish(topic, message);
            logger.debug("Published event {} to topic {}", event.getEventId(), topic);

        } catch (Exception e) {
            logger.error("Failed to publish event: {}", event, e);
            throw new RuntimeException("Failed to publish MQTT event", e);
        }
    }
}
