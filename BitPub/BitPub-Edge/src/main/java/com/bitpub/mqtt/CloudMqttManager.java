package com.bitpub.mqtt;

import com.bitpub.security.TlsUtility;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Gestore della connettività MQTT per l'integrazione tra l'Edge Node e il backend Cloud.
 * Configura il client per garantire la persistenza dei dati e la sicurezza del trasporto.
 *
 * <p>Caratteristiche principali: Sessioni durevoli, TLS mutuo e auto-reconnect.</p>
 *
 * @author Timothy (Architettura Sessioni - Fase 21)
 * @author Stefano Bellan 20054330 (Modulo Sicurezza TLS)
 */
public class CloudMqttManager {

    /**
     * Factory method per la creazione e configurazione del client MQTT Cloud.
     * Implementa la logica di "Store and Forward" necessaria per gestire l'intermittenza della rete.
     *
     * @param brokerCloudUrl L'URL del broker remoto (es. "ssl://localhost:8883").
     * @param nomeLocale     Identificativo testuale del locale per la generazione del ClientID.
     * @return {@link MqttClient} istanziato e configurato, pronto per la chiamata .connect().
     * @throws MqttException Se l'URL del broker non è valido o l'istanziazione fallisce.
     */
    public static MqttClient configuraClientCloud(String brokerCloudUrl, String nomeLocale) throws Exception {

        // Definizione ClientID statico: critico per il ripristino della sessione lato broker
        String clientIdFisso = "Edge-" + nomeLocale;

        // Utilizzo MemoryPersistence per i metadati del client
        MqttClient cloudClient = new MqttClient(brokerCloudUrl, clientIdFisso, new MemoryPersistence());

        MqttConnectOptions connOpts = new MqttConnectOptions();

        /*
         * CONFIGURAZIONE SESSIONE AVANZATA (Timothy - Fase 21)
         */

        // setCleanSession(false) abilita la "Durable Session": il broker mantiene
        // le sottoscrizioni e i messaggi QoS 1/2 anche se il client è offline.
        connOpts.setCleanSession(false);

        // Riconnessione automatica gestita dal client Paho
        connOpts.setAutomaticReconnect(true);

        // Invia un pacchetto di controllo ogni 60 secondi per confermare che il server sia vivo
        connOpts.setKeepAliveInterval(60);

        // Tempo massimo di attesa per stabilire la connessione iniziale
        connOpts.setConnectionTimeout(30);

        /*
         * CONFIGURAZIONE SICUREZZA TLS (Ref: Stefano 20054330)
         * Mutual TLS (mTLS): carichiamo l'intera catena di certificati.
         * Fix Bug #3: l'eccezione viene ora propagata invece di essere
         * catturata silenziosamente, rendendo visibile ogni problema sui certificati.
         */
        String certsBasePath = "../BitPub-Security/certs";

        // applyTlsToOptions ora dichiara throws Exception: se i certificati
        // mancano o sono corrotti, il metodo lancia e stoppiamo subito l'avvio.
        TlsUtility.applyTlsToOptions(connOpts, certsBasePath);
        System.out.println("[EDGE-INFO] TLS Setup: Handshake mTLS configurato con successo.");

        cloudClient.connect(connOpts);

        System.out.println("[EDGE] Client Cloud pronto. ID: " + clientIdFisso + " (Durable Session e Auto-Reconnect attivi)");

        return cloudClient;
    }
}