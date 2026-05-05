package com.bitpub;

import com.bitpub.edge.EdgeMqttClient;
import com.bitpub.edge.GameTableStateManager;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * FILE 15: EdgeApplication (Main)
 * Entry point per l'Edge Node.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("BitPub Edge Node avviato. Connessione al broker MQTT...");

        try {
            // 1. Istanzia lo State Manager
            GameTableStateManager stateManager = new GameTableStateManager();

            // 2. Istanzia e connette il Client MQTT
            EdgeMqttClient mqttClient = new EdgeMqttClient(stateManager);
            mqttClient.connect();

            // 3. Avvia il thread separato per l'heartbeat (ogni 15 secondi)
            ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
            heartbeatScheduler.scheduleAtFixedRate(() -> {
                if (mqttClient.getClient() != null && mqttClient.getClient().isConnected()) {
                    try {
                        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        String payload = String.format("{\"status\":\"ONLINE\",\"timestamp\":\"%s\"}", timestamp);
                        
                        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                        message.setQos(1);
                        // mqttClient.getClient().publish("bitpub/locali/1/edge/heartbeat", message); // Muted: zero internal event generation
                        System.out.println("[Edge] Segnale di presenza generato ma non inviato (Passive Edge).");
                        
                    } catch (Exception e) {
                        System.err.println("Errore invio heartbeat: " + e.getMessage());
                    }
                }
            }, 0, 15, TimeUnit.SECONDS);

            // 4. Shutdown hook per la chiusura pulita
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.err.println("Chiusura pulita dell'Edge Node in corso...");
                    heartbeatScheduler.shutdownNow();
                    mqttClient.disconnect();
                } catch (Exception ex) {
                    // Ignora le eccezioni in fase di chiusura (es. JAnsi su Windows)
                }
            }));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
