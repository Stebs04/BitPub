package com.bitpub.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Gestore della connessione MQTT dall'Edge verso il server Cloud.
 * Implementa la resilienza offline tramite Durable Session.
 */
public class CloudMqttManager {

    /**
     * Crea e configura il client MQTT per il Cloud.
     * * @param brokerCloudUrl L'indirizzo del Cloud (es. "ssl://cloud.bitpub.com:8883")
     * @param nomeLocale Il nome univoco del locale (es. "Milano-1")
     * @return Il client configurato e pronto alla connessione
     */
    public static MqttClient configuraClientCloud(String brokerCloudUrl, String nomeLocale) throws MqttException {

        // --- TIMOTHY: 1. ClientID Statico ---
        // Generiamo un ID fisso per questo specifico Edge Node.
        // Se si disconnette e si riconnette, il Cloud lo riconoscerà sempre!
        String clientIdFisso = "Edge-" + nomeLocale;

        MqttClient cloudClient = new MqttClient(brokerCloudUrl, clientIdFisso, new MemoryPersistence());

        // Configurazione delle opzioni di connessione
        MqttConnectOptions connOpts = new MqttConnectOptions();

        // --- TIMOTHY: 2. Durable Session (Clean Session = false) ---
        // TASSATIVO: Impostando false, abilitiamo la sessione persistente.
        // Fondamentale per il paradigma "Store and Forward".
        connOpts.setCleanSession(false);

        // Aggiungiamo anche la riconnessione automatica per comodità
        connOpts.setAutomaticReconnect(true);

        // --- SPAZIO PER STEFANO (Setup TLS) ---
        // Qui Stefano inserirà il codice per validare ca.crt generato dal vostro script OpenSSL
        // Esempio futuro: connOpts.setSocketFactory(StefanoTlsUtility.getSocketFactory());

        System.out.println("[EDGE] Client Cloud configurato con ID statico: " + clientIdFisso + " e CleanSession=false");

        return cloudClient;
    }
}