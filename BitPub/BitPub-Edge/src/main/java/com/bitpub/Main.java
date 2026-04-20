package com.bitpub;

import com.bitpub.buffer.MessageBuffer;
import com.bitpub.mqtt.LocalCalciobalillaSubscriber;
import com.bitpub.mqtt.BiliardoSubscriber;
import com.bitpub.mqtt.CloudMqttManager;
import com.bitpub.Cloud.InoltroCloudTask;
import org.eclipse.paho.client.mqttv3.MqttClient;

/**
 * Punto di ingresso principale per il nodo Edge del sistema BitPub.
 * Questa classe coordina l'architettura "Store and Forward", orchestrando i
 * subscriber locali (produttori) e il task di inoltro cloud (consumatore)
 * attraverso un buffer di messaggi centralizzato.
 *
 * @author Stefano Bellan 20054330
 */
public class Main {

    /**
     * Avvia i servizi core del nodo Edge. Configura la rete locale, stabilisce
     * la connessione sicura con il Cloud e inizializza i thread di elaborazione.
     *
     * @param args Argomenti passati da riga di comando (non utilizzati).
     */
    public static void main(String[] args) {
        System.out.println("=== Avvio BitPub Edge Node ===");

        // Configurazione delle coordinate di rete per il gateway locale e remoto
        String idLocale = "Locale-Milano-01";
        String localBrokerUrl = "tcp://localhost:1883";
        String cloudBrokerUrl = "ssl://cloud.bitpub.com:8883";

        try {
            /**
             * 1. INIZIALIZZAZIONE BUFFER CONDIVISO
             * Implementa il pattern Produttore-Consumatore per garantire la persistenza
             * temporanea dei log in caso di latenza o disconnessione della rete WAN.
             */
            MessageBuffer bufferCondiviso = new MessageBuffer();

            /**
             * 2. MODULO BILIARDO (Subscriber Locale)
             * Gestisce la telemetria delle imbucate tramite una connessione dedicata.
             */
            MqttClient localBiliardoClient = new MqttClient(localBrokerUrl, "Edge-Biliardo-" + idLocale);
            localBiliardoClient.connect();
            BiliardoSubscriber biliardoSub = new BiliardoSubscriber(localBiliardoClient, bufferCondiviso);
            biliardoSub.iscrivitiTopicBiliardo();
            System.out.println("[MAIN] Sottoscrizione Biliardo locale attivata.");

            /**
             * 3. MODULO CALCIOBALILLA (Subscriber Locale)
             * Monitora gli eventi dei match. Viene avviato come thread separato per
             * gestire i flussi di dati in parallelo.
             */
            LocalCalciobalillaSubscriber calciobalillaSub = new LocalCalciobalillaSubscriber(
                    bufferCondiviso, localBrokerUrl, "Edge-Calciobalilla-" + idLocale);
            calciobalillaSub.start();
            System.out.println("[MAIN] Sottoscrizione Calciobalilla locale attivata.");

            /**
             * 4. CONFIGURAZIONE CLOUD (Uplink Sicuro)
             * Inizializzazione della connessione TLS/SSL tramite il manager dedicato.
             */
            MqttClient cloudClient = CloudMqttManager.configuraClientCloud(cloudBrokerUrl, idLocale);
            System.out.println("[MAIN] Client Cloud configurato e connesso con successo.");

            /**
             * 5. AVVIO TASK DI INOLTRO (Consumatore)
             * Il thread preleva i messaggi dal buffer e li trasmette al server centrale.
             */
            InoltroCloudTask inoltroTask = new InoltroCloudTask(bufferCondiviso, cloudClient);
            Thread inoltroThread = new Thread(inoltroTask);
            inoltroThread.start();
            System.out.println("[MAIN] Servizio di Inoltro Cloud avviato.");

        } catch (Exception e) {
            /**
             * Error Handling: arresto anomalo del nodo Edge.
             * Utilizzato per identificare problemi di connettività iniziale o certificati SSL errati.
             */
            System.err.println("[MAIN] Errore critico durante l'avvio dell'Edge Node:");
            e.printStackTrace();
        }
    }
}
