package it.uniupo.pissir.bitpub.javafx.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.javafx.controller.KioskController;
import it.uniupo.pissir.bitpub.javafx.model.GameStateDto;
import javafx.application.Platform;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.UUID;

public class MqttSubscriberService {

    private final String brokerUrl;
    private final String localeId;
    private final String gameInstanceId;
    private final KioskController controller;
    private IMqttClient mqttClient;
    private final ObjectMapper objectMapper;

    public MqttSubscriberService(String brokerUrl, String localeId, String gameInstanceId, KioskController controller) {
        this.brokerUrl = brokerUrl;
        this.localeId = localeId;
        this.gameInstanceId = gameInstanceId;
        this.controller = controller;
        this.objectMapper = new ObjectMapper();
    }

    public void start() {
        try {
            String publisherId = UUID.randomUUID().toString();
            mqttClient = new MqttClient(brokerUrl, publisherId);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            mqttClient.connect(options);

            String topic = MqttTopics.getGameStateTopic(localeId, gameInstanceId);
            System.out.println("Subscribing to: " + topic);
            
            mqttClient.subscribe(topic, (t, msg) -> {
                String payload = new String(msg.getPayload());
                try {
                    GameStateDto state = objectMapper.readValue(payload, GameStateDto.class);
                    Platform.runLater(() -> controller.updateGameState(state));
                } catch (Exception e) {
                    System.err.println("Error parsing MQTT payload: " + e.getMessage());
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
}
