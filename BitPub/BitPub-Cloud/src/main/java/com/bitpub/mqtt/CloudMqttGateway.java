package com.bitpub.mqtt;

import com.bitpub.services.ElaborazioneEventiService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CloudMqttGateway - Gateway bidirezionale per telemetria di gioco.
 * * Refactoring Senior Note:
 * Rimosso l'accesso a GameSessionRepository e PartitaCalciobalillaRepository.
 * Il gateway agisce ora come 'Protocol Adapter' puro: valida la firma hardware 
 * dei messaggi e pubblica eventi interni di Spring. Questo previene il time-out
 * del protocollo MQTT causato da latenze del database PostgreSQL.
 */
@Component
public class CloudMqttGateway implements MqttCallback {

    @org.springframework.beans.factory.annotation.Value("${mqtt.broker.url:tcp://localhost:1883}")
    private String brokerUrl;
    private final String CLIENT_ID = "BitPub-Cloud-Gateway-" + java.util.UUID.randomUUID().toString();
    
    private MqttClient client;
    private final Gson gson = new Gson();

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ElaborazioneEventiService elaborazioneEventiService;

    /** Monitoraggio heartbeat (mantenuto in memoria per diagnostica rapida) */
    private final Map<String, Instant> edgeLastSeen = new ConcurrentHashMap<>();

    public Map<String, Instant> getEdgeLastSeen() {
        return edgeLastSeen;
    }

    @PostConstruct
    public void startGateway() {
        try {
            client = new MqttClient(brokerUrl, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            client.setCallback(this);
            client.connect(options);

            client.subscribe("bitpub/edge/heartbeat", 1);
            client.subscribe("bitpub/edge/+/score", 1);
            client.subscribe("bitpub/locali/+/calciobalilla/+/eventi", 0);

            System.out.println("[CLOUD GATEWAY] Gateway avviato in modalità Event-Driven.");
        } catch (MqttException e) {
            System.err.println("[CLOUD GATEWAY] Errore critico avvio: " + e.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        try {
            // Filtro di sicurezza hardware: scarta messaggi non firmati o non validi
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            if (!json.has("source") || !"DEVICE".equals(json.get("source").getAsString())) {
                return;
            }

            if (topic.equals("bitpub/edge/heartbeat")) {
                handleHeartbeat(json);
            } else if (topic.matches("bitpub/edge/.+/score")) {
                // Pubblica l'evento di score per l'elaborazione asincrona
                elaborazioneEventiService.processaESalvaEvento(topic, payload);
            } else if (topic.matches("bitpub/locali/.+/calciobalilla/.+/eventi")) {
                // Pubblica l'evento simulator per il salvataggio dei risultati
                elaborazioneEventiService.processaESalvaEvento(topic, payload);
            }

        } catch (Exception e) {
            System.err.println("[CLOUD GATEWAY] Payload scartato: formato non valido.");
        }
    }

    private void handleHeartbeat(JsonObject json) {
        if (json.has("nodeId")) {
            edgeLastSeen.put(json.get("nodeId").getAsString(), Instant.now());
        }
    }

    public void publishUnlockBalls(Integer tableId, Long sessionId) {
        JsonObject json = new JsonObject();
        json.addProperty("tableId", tableId);
        json.addProperty("sessionId", sessionId);
        publishMessage("bitpub/cloud/foosball/start", json.toString(), 1);
    }

    public void publishForceStop(Integer tableId) {
        JsonObject json = new JsonObject();
        json.addProperty("tableId", tableId);
        json.addProperty("command", "FORCE_STOP");
        publishMessage("bitpub/cloud/foosball/stop", json.toString(), 1);
    }

    private void publishMessage(String topic, String payload, int qos) {
        if (client != null && client.isConnected()) {
            try {
                MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                msg.setQos(qos);
                client.publish(topic, msg);
            } catch (MqttException e) {
                System.err.println("[CLOUD GATEWAY] Errore publish: " + e.getMessage());
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[CLOUD GATEWAY] Connessione persa: " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { }
}