package com.bitpub.mqtt;

import com.bitpub.events.EdgeStatusUpdateEvent;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * MqttAdminGateway - Gestore MQTT per le funzionalità di amministrazione centralizzate.
 * * Refactoring Senior Note:
 * È stata rimossa la dipendenza diretta da EdgeStatusRepository per evitare blocchi
 * del thread MQTT durante le operazioni di persistenza. Il gateway ora pubblica
 * un 'EdgeStatusUpdateEvent' delegando l'elaborazione al layer di servizio asincrono.
 */
@Component
public class MqttAdminGateway implements MqttCallback {

    private IMqttClient client;
    private final String brokerUrl = "tcp://localhost:1883";
    private final String clientId = "BitPub-Cloud-Admin-" + java.util.UUID.randomUUID().toString();

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @PostConstruct
    public void init() {
        try {
            client = new MqttClient(brokerUrl, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            client.setCallback(this);
            client.connect(options);

            client.subscribe("bitpub/locali/+/status");
            System.out.println("[MQTT ADMIN] Connesso e in ascolto sui canali di stato locali.");

        } catch (MqttException e) {
            System.err.println("[MQTT ADMIN] Errore connessione: " + e.getMessage());
        }
    }

    public void publish(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(2); // QoS 2 garantisce la consegna dei comandi critici
            client.publish(topic, message);
        } catch (MqttException e) {
            System.err.println("[MQTT ADMIN] Errore pubblicazione: " + e.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());

        // Parsing del topic per estrarre il venueId (es: bitpub/locali/Milano-01/status)
        String[] parts = topic.split("/");
        if (parts.length >= 3 && topic.endsWith("/status")) {
            String venueId = parts[2];
            String status = payload.contains("ONLINE") ? "ONLINE" : "OFFLINE";

            // Pubblicazione dell'evento applicativo disaccoppiato
            eventPublisher.publishEvent(new EdgeStatusUpdateEvent(this, venueId, status));
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("[MQTT ADMIN] Connessione persa: " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { }
}