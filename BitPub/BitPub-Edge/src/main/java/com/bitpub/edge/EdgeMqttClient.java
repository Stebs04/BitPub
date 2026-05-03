package com.bitpub.edge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * FILE 15 (Update): EdgeMqttClient
 * Client MQTT per l'Edge Node. Orchestra i comandi Cloud e instrada 
 * la coda degli eventi del simulatore stocastico.
 */
public class EdgeMqttClient implements MqttCallback {

    private static final String BROKER_URL = "ssl://localhost:8883";
    private static final String CLIENT_ID = "BitPub-Edge-Node-1";
    private static final String CERTS_BASE_PATH = "../BitPub-Security/certs";

    private MqttClient client;
    private final GameTableStateManager stateManager;
    
    // Coda bloccante thread-safe per accogliere gli eventi dal Simulatore
    private final BlockingQueue<FoosballEvent> eventQueue = new LinkedBlockingQueue<>();
    private Thread simulatorThread;
    private SimCalciobalilla activeSimulator;

    private final Gson gson = new Gson();

    public EdgeMqttClient(GameTableStateManager stateManager) {
        this.stateManager = stateManager;
    }

    public void connect() {
        try {
            client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            // TODO: Decommenta questa linea se hai già configurato TlsUtility in questo modulo
            com.bitpub.security.TlsUtility.applyTlsToOptions(options, CERTS_BASE_PATH);

            client.setCallback(this);
            client.connect(options);
            System.out.println("[EDGE NODE] Connesso al broker " + BROKER_URL);

            client.subscribe("bitpub/cloud/foosball/start", 1);
            client.subscribe("bitpub/cloud/foosball/force-stop", 1);

            // Avviamo il thread consumatore che leggerà dalla coda e pubblicherà via MQTT
            startEventPublisher();

        } catch (MqttException e) {
            System.err.println("[EDGE NODE] Errore connessione MQTT: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[EDGE NODE] Errore TLS o generico: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {
                if (simulatorThread != null) {
                    simulatorThread.interrupt();
                }
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

    /**
     * Pattern Produttore-Consumatore: Un thread in background estrae di continuo gli eventi
     * inseriti in coda da SimCalciobalilla e li pubblica verso il Cloud in modo asincrono.
     */
    private void startEventPublisher() {
        Thread publisherThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    FoosballEvent event = eventQueue.take(); // Bloccante: attende finché non c'è un evento
                    
                    if (client != null && client.isConnected()) {
                        String payload = gson.toJson(event);
                        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                        message.setQos(1);
                        client.publish("bitpub/edge/" + event.getTableId() + "/score", message);
                        System.out.println("[EDGE NODE] -> MQTT OUT: " + payload);
                        
                        // Libera il tavolo se il simulatore ha inviato un evento finale
                        if ("FINISHED".equals(event.getStatus()) || "FORCE_STOPPED".equals(event.getStatus())) {
                            stateManager.setFree(event.getTableId());
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("[EDGE NODE] Publisher thread interrotto.");
                    Thread.currentThread().interrupt();
                } catch (MqttException e) {
                    System.err.println("[EDGE NODE] Errore pubblicazione evento: " + e.getMessage());
                }
            }
        });
        publisherThread.setDaemon(true);
        publisherThread.start();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        System.out.println("\n[EDGE NODE] <- MQTT IN (" + topic + "): " + payload);

        try {
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            Integer tableId = json.has("tableId") ? json.get("tableId").getAsInt() : null;

            if (tableId != null && tableId == 1) {
                if (topic.equals("bitpub/cloud/foosball/start")) {
                    if (stateManager.isOccupied(tableId)) {
                        System.out.println("[EDGE NODE] Avviso: il Tavolo " + tableId + " risulta già occupato.");
                        return;
                    }
                    // 1. Aggiorna stato
                    stateManager.setOccupied(tableId);
                    
                    // 2. Avvia nuovo thread stocastico passando la coda condivisa
                    activeSimulator = new SimCalciobalilla(tableId, eventQueue);
                    simulatorThread = new Thread(activeSimulator);
                    simulatorThread.start();
                } 
                else if (topic.equals("bitpub/cloud/foosball/force-stop")) {
                    System.out.println("[EDGE NODE] Comando d'emergenza: FORCE-STOP per tavolo " + tableId);
                    // 1. Interrompe il thread del simulatore
                    if (simulatorThread != null && simulatorThread.isAlive()) {
                        simulatorThread.interrupt();
                    }
                    // 2. Aggiorna stato (anche se lo farà il publisher, forziamo qui per sicurezza)
                    stateManager.setFree(tableId);
                }
            }
        } catch (Exception e) {
            System.err.println("[EDGE NODE] Errore parsing comando MQTT: " + e.getMessage());
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
