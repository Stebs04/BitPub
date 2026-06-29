package com.bitpub.mqtt.retry;

import com.bitpub.mqtt.publisher.MqttPublisher;
import com.bitpub.mqtt.registry.MqttTopicRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterRetryService {

    private final MqttPublisher mqttPublisher;

    /**
     * Publishes a payload to the specific Retry topic for an original topic.
     * This is useful when a service wants to attempt a retry of a failed message
     * either automatically or triggered manually.
     *
     * @param originalTopic The original topic the message failed on
     * @param payload       The payload to retry
     */
    public void sendToRetry(String originalTopic, Object payload) {
        String retryTopic = MqttTopicRegistry.retry(originalTopic);
        log.info("Sending message to retry topic: {}", retryTopic);
        mqttPublisher.publish(retryTopic, payload);
    }
}
