package com.bitpub.mqtt;

import com.bitpub.cloud.security.CloudTlsUtility;
import com.bitpub.repository.GameSessionEntity;
import com.bitpub.repository.GameSessionRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway di comunicazione MQTT Cloud-Side bidirezionale.
 */
@Component
public class CloudMqttGateway implements MqttCallback {

    // Configurazione endpoint TLS come prima
    private static final String BROKER_URL = "ssl://localhost:8883";
    private final String CLIENT_ID = "BitPub-Cloud-Gateway-" + java.util.UUID.randomUUID().toString();
    private static final String CERTS_BASE_PATH = "../BitPub-Security/certs";

    private MqttClient client;
    private final Gson gson = new Gson();

    // Mappa Thread-Safe per tracciare l'heartbeat dei vari Edge Node (es. chiave "1")
    private final Map<String, Instant> edgeLastSeen = new ConcurrentHashMap<>();

    @Autowired
    private GameSessionRepository gameSessionRepository;

    public Map<String, Instant> getEdgeLastSeen() {
        return edgeLastSeen;
    }

    @PostConstruct
    public void startGateway() {
        try {
            client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
            options.setAutomaticReconnect(true);

            // Mantiene il supporto TLS custom pre-esistente
            CloudTlsUtility.applyTlsToOptions(options, CERTS_BASE_PATH);

            client.setCallback(this);
            client.connect(options);

            // Sottoscrizioni richieste dal prompt con QoS 1
            client.subscribe("bitpub/edge/heartbeat", 1);
            client.subscribe("bitpub/edge/+/score", 1);

            System.out.println("[CLOUD GATEWAY] Connesso e in ascolto su heartbeat e score...");
        } catch (MqttException e) {
            System.err.println("[CLOUD GATEWAY] Errore critico durante l'avvio: " + e.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        System.out.println("[MQTT IN] Topic: " + topic + " | Payload: " + payload);

        if (topic.equals("bitpub/edge/heartbeat")) {
            // Aggiorna l'orario di ultimo ping per l'Edge
            try {
                JsonObject json = gson.fromJson(payload, JsonObject.class);
                if (json.has("nodeId")) {
                    String nodeId = json.get("nodeId").getAsString();
                    edgeLastSeen.put(nodeId, Instant.now());
                }
            } catch (Exception e) {
                System.err.println("Errore parsing heartbeat: " + e.getMessage());
            }

        } else if (topic.matches("bitpub/edge/.+/score")) {
            // Ricezione punteggio e salvataggio DB
            try {
                JsonObject json = gson.fromJson(payload, JsonObject.class);
                Long sessionId = json.get("sessionId").getAsLong();
                int scoreBlue = json.get("scoreBlue").getAsInt();
                int scoreRed = json.get("scoreRed").getAsInt();

                Optional<GameSessionEntity> sessionOpt = gameSessionRepository.findById(sessionId);
                if (sessionOpt.isPresent()) {
                    GameSessionEntity session = sessionOpt.get();
                    if ("IN_PROGRESS".equals(session.getStatus())) {
                        session.setScoreBlue(scoreBlue);
                        session.setScoreRed(scoreRed);
                        
                        // Controllo vittoria fittizio (es. primo che arriva a 10 vince)
                        if (scoreBlue >= 10 || scoreRed >= 10) {
                            session.setStatus("FINISHED");
                            session.setFinishedAt(LocalDateTime.now());
                            System.out.println("[GAME] Partita terminata!");
                        }
                        gameSessionRepository.save(session);
                    }
                }
            } catch (Exception e) {
                System.err.println("Errore parsing o salvataggio score: " + e.getMessage());
            }
        }
    }

    /**
     * Pubblica il comando per sbloccare le palline del calciobalilla (Start Partita)
     */
    public void publishUnlockBalls(Integer tableId) {
        JsonObject json = new JsonObject();
        json.addProperty("tableId", tableId);
        publishMessage("bitpub/cloud/foosball/start", json.toString(), 1);
    }

    /**
     * Pubblica il comando per forzare la chiusura di un tavolo (Admin Force Stop)
     */
    public void publishForceStop(Integer tableId) {
        JsonObject json = new JsonObject();
        json.addProperty("tableId", tableId);
        publishMessage("bitpub/cloud/foosball/force-stop", json.toString(), 1);
    }

    private void publishMessage(String topic, String payload, int qos) {
        if (client != null && client.isConnected()) {
            try {
                MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                msg.setQos(qos);
                client.publish(topic, msg);
                System.out.println("[MQTT OUT] Topic: " + topic + " | Payload: " + payload);
            } catch (MqttException e) {
                System.err.println("Errore publish MQTT: " + e.getMessage());
            }
        } else {
            System.err.println("Impossibile inviare messaggio MQTT: client non connesso.");
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[CLOUD GATEWAY] Connessione persa (il client tenterà il ripristino): " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Nessuna azione speciale richiesta per la conferma di consegna base
    }
}
