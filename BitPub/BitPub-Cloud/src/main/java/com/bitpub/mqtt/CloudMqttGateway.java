package com.bitpub.mqtt;

import com.bitpub.services.PersistenceService;
import com.bitpub.utils.MqttCalciobalillaTopics;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * Gateway di ricezione Cloud-side.
 * Agisce come ponte (Bridge) tra il protocollo MQTT e il database relazionale.
 *
 * <p>Il servizio è configurato come Singleton gestito da Spring e implementa il pattern
 * Callback per l'elaborazione asincrona dei messaggi in arrivo dai nodi Edge.</p>
 *
 * @author Stefano Bellan 20054330
 */
@Service
public class CloudMqttGateway implements MqttCallback {

    // Configurazione endpoint (Nota: spostare preferibilmente in application.properties/yml)
    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String CLIENT_ID = "BitPub-Cloud-Gateway";

    private MqttClient client;

    /**
     * Servizio di persistenza per l'interfacciamento con PostgreSQL via JPA/Hibernate.
     */
    @Autowired
    private PersistenceService persistenceService;

    /**
     * Lifecycle Hook di Spring: inizializza la connessione MQTT dopo l'iniezione delle dipendenze.
     * Configura una sessione persistente per garantire la ricezione dei messaggi QoS 1/2
     * anche in caso di downtime temporaneo del gateway.
     */
    @PostConstruct
    public void startGateway() {
        try {
            // MemoryPersistence lato Cloud: lo stato della sessione è mantenuto dal Broker
            client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();

            /*
             * STRATEGIA DI RESILIENZA:
             * CleanSession(false) + QoS 1 (in subscribe) garantiscono il "at least once delivery".
             * La riconnessione automatica gestisce i micro-downtime di rete.
             */
            options.setCleanSession(false);
            options.setAutomaticReconnect(true);

            client.setCallback(this);
            client.connect(options);

            // Sottoscrizione con QoS 1 per attivare il meccanismo di acknowledgment del broker
            client.subscribe(MqttCalciobalillaTopics.TOPIC_CLOUD, 1);

            System.out.println("[CLOUD GATEWAY] Connesso e in ascolto sui topic dei locali...");
        } catch (MqttException e) {
            // LOGICA DI RECOVERY: In un ambiente reale qui scatterebbe un alert (es. Prometheus/Sentry)
            System.err.println("[CLOUD GATEWAY] Errore critico durante l'avvio: " + e.getMessage());
        }
    }

    /**
     * Handler asincrono per i messaggi in ingresso.
     * Converte il payload binario in stringa e delega la logica di business al PersistenceService.
     *
     * @param topic   Topic sorgente (formato gerarchico bitpub/locali/+/...)
     * @param message Oggetto contenente il payload e i metadati MQTT
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Conversione sicura del payload
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        /*
         * DELEGA PERSISTENZA:
         * Il salvataggio avviene nel thread di callback di Paho.
         * Assicurarsi che salvaMessaggio() gestisca correttamente le transazioni DB.
         */
        persistenceService.salvaMessaggio(topic, payload);
    }

    /**
     * Callback per la perdita della connessione.
     * Nota: setAutomaticReconnect(true) tenterà il ripristino autonomamente.
     */
    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[CLOUD GATEWAY] Connessione persa (il client tenterà il ripristino): " + cause.getMessage());
    }

    /**
     * Invocata quando un messaggio inviato dal gateway è confermato dal broker.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Attualmente il Gateway opera principalmente come Consumer (Subscriber)
    }
}
