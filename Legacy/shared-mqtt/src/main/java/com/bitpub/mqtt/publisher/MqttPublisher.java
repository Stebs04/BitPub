package com.bitpub.mqtt.publisher;

/**
 * Abstraction for publishing MQTT messages.
 */
public interface MqttPublisher {

    /**
     * Publishes a message to the specified topic with default QoS (1) and not retained.
     *
     * @param topic   The target MQTT topic
     * @param payload The object to serialize and send
     */
    void publish(String topic, Object payload);

    /**
     * Publishes a message with specific QoS.
     *
     * @param topic   The target MQTT topic
     * @param payload The object to serialize and send
     * @param qos     Quality of Service (0, 1, 2)
     */
    void publish(String topic, Object payload, int qos);

    /**
     * Publishes a message with specific QoS and Retained flag.
     *
     * @param topic    The target MQTT topic
     * @param payload  The object to serialize and send
     * @param qos      Quality of Service (0, 1, 2)
     * @param retained True if the message should be retained by the broker
     */
    void publish(String topic, Object payload, int qos, boolean retained);
}
