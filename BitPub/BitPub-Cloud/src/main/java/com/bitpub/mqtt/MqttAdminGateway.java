package com.bitpub.mqtt;

import com.bitpub.cloud.repository.EdgeStatusEntity;
import com.bitpub.repository.EdgeStatusRepository;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * Gestore MQTT per le funzionalità di amministrazione centralizzate del Cloud.
 * Agisce come gateway per monitorare lo stato di connettività degli Edge locali
 * e per l'invio di comandi critici di sblocco risorse.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
@Component
public class MqttAdminGateway implements MqttCallback {

    /** Istanza del client per la comunicazione con il broker MQTT. */
    private IMqttClient client;

    /** Indirizzo del broker MQTT (tipicamente Mosquitto). */
    private final String brokerUrl = "tcp://localhost:1883";

    /** Identificativo univoco del client per il broker. */
    private final String clientId = "BitPub-Cloud-Admin";

    /** Repository per la persistenza dello stato operativo delle sedi. */
    @Autowired
    private EdgeStatusRepository statusRepository;

    /**
     * Inizializza la connessione al broker MQTT e configura le sottoscrizioni.
     * Metodo eseguito automaticamente dopo la creazione del bean Spring.
     */
    @PostConstruct
    public void init() {
        try {
            client = new MqttClient(brokerUrl, clientId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // Avvio sessione pulita
            options.setAutomaticReconnect(true); // Riconnessione automatica in caso di caduta linea

            client.setCallback(this);
            client.connect(options);

            // Sottoscrizione ai topic di stato utilizzando la wildcard "+" per intercettare tutti i locali
            client.subscribe("bitpub/locali/+/status");
            System.out.println("MQTT Admin Gateway connesso e in ascolto sui canali locali...");

        } catch (MqttException e) {
            System.err.println("Errore critico nella connessione MQTT: " + e.getMessage());
        }
    }

    /**
     * Pubblica un messaggio su un determinato topic.
     * Utilizzato primariamente per l'invio di comandi "FORCE_UNLOCK" agli emulatori Edge.
     *
     * @param topic   Il percorso del canale MQTT.
     * @param payload Il contenuto del messaggio da inviare.
     */
    public void publish(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            // QoS 2: Garantisce che il comando venga ricevuto esattamente una volta (fondamentale per sblocchi hardware)
            message.setQos(2);
            client.publish(topic, message);
        } catch (MqttException e) {
            System.err.println("Errore durante la pubblicazione del messaggio: " + e.getMessage());
        }
    }

    /**
     * Callback invocata alla ricezione di un messaggio sui topic sottoscritti.
     * Estrae l'identificativo del locale dal topic e ne aggiorna lo stato nel DB.
     *
     * @param topic   Il topic di origine (es: bitpub/locali/Milano-01/status).
     * @param message Il contenuto del messaggio ricevuto.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());

        // Parsing del topic per l'estrazione della variabile dinamica venueId
        String[] parts = topic.split("/");
        if (parts.length >= 3 && topic.endsWith("/status")) {
            String venueId = parts[2];

            // Logica di determinazione dello stato basata sul contenuto del payload
            String status = payload.contains("ONLINE") ? "ONLINE" : "OFFLINE";

            // Sincronizzazione dello stato nel database PostgreSQL tramite Repository
            EdgeStatusEntity entity = new EdgeStatusEntity(venueId, status);
            statusRepository.save(entity);

            System.out.println("Aggiornamento stato persistito per locale: " + venueId + " -> " + status);
        }
    }

    /**
     * Notifica la perdita di connessione con il broker.
     */
    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Avviso: Connessione MQTT persa. Causa: " + cause.getMessage());
    }

    /**
     * Metodo di callback per la conferma dell'invio (non implementato per la sola ricezione).
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Logica opzionale per conferma ricezione messaggi pubblicati
    }
}
