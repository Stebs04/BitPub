package com.bitpub.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestratore specializzato per il bootstrapping della connessione MQTT verso l'infrastruttura Cloud.
 * Nasconde la complessità architetturale legata all'implementazione dei pattern di sicurezza
 * (autenticazione mutua TLS) e di continuità del servizio (Durable Sessions e QoS).
 * Isola rigorosamente la logica di configurazione dal resto dell'Edge Node, fornendo
 * ai componenti superiori un socket Paho già autenticato, sbloccato e pronto per la sottoscrizione o pubblicazione.
 * @author Timothy (Architettura Sessioni - Fase 21)
 * @author Stefano Bellan 20054330
 */
public class CloudMqttManager {

    // Tracer operativo interfacciato con Logback
    private static final Logger logger = LoggerFactory.getLogger(CloudMqttManager.class);

    /**
     * Costruttore factory (statico) preposto all'allocazione logica del socket di trasporto.
     * Allinea i parametri di latenza, impone la conservazione dello stato sul broker (Clean Session = false)
     * e avvolge la connessione all'interno dell'involucro crittografico fornito tramite SSLContext.
     *
     * @param brokerHost Hostname puro (FQDN o indirizzo IP) del server Mosquitto centrale
     * @param nomeLocale Valore convenzionale stringa per generare il prefisso del ClientID
     * @param sslContext Contesto crittografato pre-valutato contenente KeyManager e TrustManager
     * @return Una connessione logica asincrona operativa verso la control-plane
     * @throws Exception Se la sintassi dell'URI non risulta valida o l'handshake del tunnel TLS fallisce
     */
    public static MqttClient configuraClientCloud(String brokerHost, String nomeLocale, SSLContext sslContext) throws Exception {

        // Composizione formale del Client ID: tassativo che resti costante tra un riavvio e l'altro.
        // Se il client cambiasse identificativo a ogni avvio, il broker (Mosquitto) sarebbe incapace di
        // ricollegare il socket alla Durable Session orfana, scartando i messaggi accodati durante il blackout.
        String clientIdFisso = "Edge-" + nomeLocale;

        // Composizione hardcoded dello schema e della porta standard per connessioni MQTT-S
        String brokerCloudUrl = "ssl://" + brokerHost + ":8883";

        MqttConnectOptions connOpts = new MqttConnectOptions();

        /*
         * REGOLA DI INGEGNERIA N°1: DURABLE SESSION
         * L'impostazione del flag a falso ordina al broker di conservare l'albero delle subscription e la
         * coda dei messaggi Quality of Service 1/2 quando questo client va in timeout, preparandosi a riversarli
         * istantaneamente al ripristino del tunnel.
         */
        connOpts.setCleanSession(false);

        /*
         * REGOLA DI INGEGNERIA N°2: RESILIENZA E TCP/IP TUNING
         * Dimensionamento proattivo dei timer TCP per consentire attraversamenti sicuri
         * in reti inaffidabili o pesantemente sottoposte a NAT/Firewall cellulari (reti mobili 4G/5G).
         */
        connOpts.setConnectionTimeout(30);  // Intervallo di tolleranza all'handshake SYN-ACK (30s)
        connOpts.setKeepAliveInterval(60); // Cadenza fisiologica dei ping MQTT per mantenere calda la sessione (60s)

        // Attivazione dell'algoritmo intrinseco Paho per eseguire exponential backoff retry in autonomia
        connOpts.setAutomaticReconnect(true);

        /*
         * REGOLA DI INGEGNERIA N°3: CRITTOGRAFIA DI LIVELLO TRASPORTO (mTLS)
         * Incorporazione del contesto di autenticazione asimmetrico: trasforma un payload clear-text
         * in uno stream indecifrabile per attacchi di tipo Man In The Middle.
         */
        if (sslContext != null) {
            connOpts.setSocketFactory(sslContext.getSocketFactory());
            logger.info("[CLOUD MQTT] SSLSocketFactory iniettata correttamente per porta 8883.");
        } else {
            throw new IllegalStateException("SSLContext nullo: impossibile stabilire connessione sicura.");
        }

        // Allocazione dell'istanza client imponendo uno store logico temporaneo allocato esclusivamente in memoria RAM (MemoryPersistence)
        MqttClient cloudClient = new MqttClient(brokerCloudUrl, clientIdFisso, new MemoryPersistence());

        // Innesco del processo di connessione sincronizzando la thread d'avvio col demone della JVM
        logger.info("[CLOUD MQTT] Tentativo di connessione a {} (ID: {})", brokerCloudUrl, clientIdFisso);
        cloudClient.connect(connOpts);

        logger.info("[CLOUD MQTT] Connessione stabilita con successo. Durable Session e mTLS attivi.");

        return cloudClient;
    }
}