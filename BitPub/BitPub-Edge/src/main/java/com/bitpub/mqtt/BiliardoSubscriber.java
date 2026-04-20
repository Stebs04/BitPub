package com.bitpub.mqtt;

import com.bitpub.buffer.MessageBuffer;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.bitpub.utils.BiliardoTopicConstants;

public class BiliardoSubscriber {

    private final MqttClient mqttClient;
    private final MessageBuffer buffer;

    public BiliardoSubscriber(MqttClient client, MessageBuffer buffer) {
        this.mqttClient = client;
        this.buffer = buffer;
    }

    public void iscrivitiTopicBiliardo() {
        try {
            mqttClient.subscribe(BiliardoTopicConstants.TOPIC_IMBUCATE, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    System.out.println("[EDGE] Ricevuta imbucata dal topic " + topic + ": " + payload);

                    // Inserisce il messaggio nel buffer condiviso (thread-safe tramite synchronized)
                    buffer.push(payload);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}