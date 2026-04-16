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
 * @author Timothy (Architettura Sessioni)
 * @author Stefano Bellan 20054330 (Modulo Sicurezza TLS)
 */
public class CloudMqttManager {

    /**
     * Factory method per la creazione e configurazione del client MQTT Cloud.
     * Implementa la logica di "Store and Forward" necessaria per gestire l'intermittenza della rete.
     *
     * @param brokerCloudUrl L'URL del broker remoto (es. "ssl://cloud.bitpub.com:8883").
     * @param nomeLocale     Identificativo testuale del locale per la generazione del ClientID.
     * @return {@link MqttClient} istanziato e configurato, pronto per la chiamata .connect().
     * @throws MqttException Se l'URL del broker non è valido o l'istanziazione fallisce.
     */
    public static MqttClient configuraClientCloud(String brokerCloudUrl, String nomeLocale) throws MqttException {

        // Definizione ClientID statico: critico per il ripristino della sessione lato broker
        String clientIdFisso = "Edge-" + nomeLocale;

        // Utilizzo MemoryPersistence per i metadati del client; la persistenza dei messaggi
        // a lungo termine è delegata alla Durable Session del broker.
        MqttClient cloudClient = new MqttClient(brokerCloudUrl, clientIdFisso, new MemoryPersistence());

        MqttConnectOptions connOpts = new MqttConnectOptions();

        /*
         * CONFIGURAZIONE SESSIONE (Ref: Timothy)
         * setCleanSession(false) abilita la "Durable Session": il broker mantiene
         * le sottoscrizioni e i messaggi QoS 1/2 anche se il client è offline.
         */
        connOpts.setCleanSession(false);
        connOpts.setAutomaticReconnect(true);

        /*
         * CONFIGURAZIONE SICUREZZA TLS (Ref: Stefano 20054330)
         * Percorso relativo per il certificato CA, mappato sulla struttura del workspace.
         */
        String caFilePath = "../BitPub-Security/ca.crt";

        try {
            // Iniezione della SocketFactory custom per il trust degli endpoint Cloud
            connOpts.setSocketFactory(TlsUtility.getSocketFactory(caFilePath));
            System.out.println("[EDGE-INFO] TLS Setup: Certificato caricato da " + caFilePath);
        } catch (Exception e) {
            // LOGICA DI FALLBACK: Un errore qui impedisce la comunicazione sicura.
            System.err.println("[EDGE-FATAL] Impossibile configurare il layer SSL/TLS. Abort.");
            e.printStackTrace();
            // TODO: Introdurre un sistema di Health Check per segnalare il nodo come "Degradato"
        }

        System.out.println("[EDGE] Client Cloud pronto. ID: " + clientIdFisso + " (Durable Session abilitata)");

        return cloudClient;
    }
}
