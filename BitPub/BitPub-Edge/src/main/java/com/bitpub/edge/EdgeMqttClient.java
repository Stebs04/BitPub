package com.bitpub.edge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.nio.charset.StandardCharsets;

/**
 * FILE 15 (Update): EdgeMqttClient
 * Client MQTT per l'Edge Node. Orchestra i comandi Cloud e instrada 
 * la coda degli eventi del simulatore stocastico.
 */
public class EdgeMqttClient implements MqttCallback {

    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String CLIENT_ID = "BitPub-Edge-Node-1";

    private MqttClient client;
    private final GameTableStateManager stateManager;
    


    private final Gson gson = com.bitpub.utils.JsonManager.getGson();

    public EdgeMqttClient(GameTableStateManager stateManager) {
        this.stateManager = stateManager;
    }

    public void connect() {
        try {
            client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            // Plain TCP, nessuna TLS in sviluppo locale

            client.setCallback(this);
            client.connect(options);
            System.out.println("[EDGE NODE] Connesso al broker " + BROKER_URL);

            client.subscribe("bitpub/cloud/foosball/start", 1);
            client.subscribe("bitpub/cloud/foosball/force-stop", 1);
            
            // Iscrizione agli eventi del simulatore
            client.subscribe("bitpub/locali/+/calciobalilla/+/eventi", 1);

        } catch (MqttException e) {
            System.err.println("[EDGE NODE] Errore connessione MQTT: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[EDGE NODE] Errore generico durante la connessione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {

                client.disconnect();
                System.out.println("[EDGE NODE] Disconnesso dal broker.");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public MqttClient getClient() {
        return client;
    }



    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        System.out.println("\n[EDGE NODE] <- MQTT IN (" + topic + "): " + payload);

        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();

            if (topic.contains("/calciobalilla/") && topic.endsWith("/eventi")) {
                // Estrazione dati dal simulatore
                String[] parts = topic.split("/");
                int tableId = 1; // Default
                if (parts.length >= 5) {
                    try {
                        tableId = Integer.parseInt(parts[4]);
                    } catch (NumberFormatException ignored) {}
                }

                int goalBlu = json.has("goalBlu") ? json.get("goalBlu").getAsInt() : 0;
                int goalRossi = json.has("goalRossi") ? json.get("goalRossi").getAsInt() : 0;
                int rullate = json.has("totaleRullate") ? json.get("totaleRullate").getAsInt() : 0;
                int durata = json.has("durataMediaPallinaSecondi") ? json.get("durataMediaPallinaSecondi").getAsInt() : 0;
                
                String status = "IN_PROGRESS";
                String winner = null;
                if (json.has("orarioFine") && !json.get("orarioFine").isJsonNull()) {
                    status = "FINISHED";
                    winner = goalBlu > goalRossi ? "BLUE" : "RED";
                } else if (goalBlu >= 10 || goalRossi >= 10) {
                    status = "FINISHED";
                    winner = goalBlu >= 10 ? "BLUE" : "RED";
                }

                // Inoltro al cloud come FoosballEvent
                FoosballEvent event = new FoosballEvent(
                        tableId, null, "GOAL", goalBlu, goalRossi, status, winner, rullate, durata
                );
                
                String forwardPayload = gson.toJson(event);
                MqttMessage forwardMessage = new MqttMessage(forwardPayload.getBytes(StandardCharsets.UTF_8));
                forwardMessage.setQos(1);
                client.publish("bitpub/edge/" + tableId + "/score", forwardMessage);
                System.out.println("[EDGE NODE] -> MQTT OUT (forwarded): " + forwardPayload);

                if ("FINISHED".equals(status) || "FORCE_STOPPED".equals(status)) {
                    stateManager.setFree(tableId);
                }
            } else {
                Integer tableId = json.has("tableId") ? json.get("tableId").getAsInt() : null;
                if (tableId != null && tableId == 1) {
                    if (topic.equals("bitpub/cloud/foosball/start")) {
                        if (!stateManager.isOccupied(tableId)) {
                            stateManager.setOccupied(tableId);
                        }
                    } else if (topic.equals("bitpub/cloud/foosball/force-stop")) {
                        stateManager.setFree(tableId);
                        FoosballEvent event = new FoosballEvent(
                                tableId, null, "FORCE_STOPPED", 0, 0, "FORCE_STOPPED", null, 0, 0
                        );
                        String forwardPayload = gson.toJson(event);
                        MqttMessage forwardMessage = new MqttMessage(forwardPayload.getBytes(StandardCharsets.UTF_8));
                        forwardMessage.setQos(1);
                        client.publish("bitpub/edge/" + tableId + "/score", forwardMessage);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[EDGE NODE] Errore parsing/forwarding: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[EDGE NODE] Connessione MQTT persa.");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
