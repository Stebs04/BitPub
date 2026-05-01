package com.bitpub;

import com.bitpub.buffer.MessageBuffer;
import com.bitpub.mqtt.LocalCalciobalillaSubscriber;
import com.bitpub.mqtt.BiliardoSubscriber;
import com.bitpub.mqtt.CloudMqttManager;
import com.bitpub.Cloud.InoltroCloudTask;
import com.bitpub.edge.AdminCommandListener;
import com.bitpub.edge.HeartbeatTask;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Punto di ingresso principale per il nodo Edge del sistema BitPub.
 * Coordina l'architettura "Store and Forward", orchestrando i subscriber locali
 * e il task di inoltro cloud attraverso un buffer di messaggi centralizzato.
 *
 * Implementa meccanismi di robustezza quali Last Will and Testament (LWT),
 * Sessioni Durevoli e monitoraggio periodico tramite Heartbeat.
 *
 * @author Stefano Bellan 20054330
 */
public class Main {

    /**
     * Metodo di avvio del sistema Edge.
     * Configura i client MQTT locali e remoti e avvia i thread di monitoraggio.
     *
     * @param args Argomenti della riga di comando.
     */
    public static void main(String[] args) {
        System.out.println("=== Avvio BitPub Edge Node ===");

        // Parametri di configurazione dell'identità locale e dei broker di riferimento
        String idLocale = "Locale-Milano-01";
        String localBrokerUrl = "tcp://localhost:1883";
        String cloudBrokerUrl = "ssl://localhost:8883";

        try {
            // 1. INIZIALIZZAZIONE BUFFER CONDIVISO (Architettura Produttore-Consumatore)
            MessageBuffer bufferCondiviso = new MessageBuffer();

            // 2. MODULO BILIARDO: Ricezione telemetria locale
            MqttClient localBiliardoClient = new MqttClient(localBrokerUrl, "Edge-Biliardo-" + idLocale);
            localBiliardoClient.connect();
            BiliardoSubscriber biliardoSub = new BiliardoSubscriber(localBiliardoClient, bufferCondiviso);
            biliardoSub.iscrivitiTopicBiliardo();
            System.out.println("[MAIN] Sottoscrizione Biliardo locale attivata.");

            // 3. MODULO CALCIOBALILLA: Ricezione telemetria locale
            LocalCalciobalillaSubscriber calciobalillaSub = new LocalCalciobalillaSubscriber(
                    bufferCondiviso, localBrokerUrl, "Edge-Calciobalilla-" + idLocale);
            calciobalillaSub.start();
            System.out.println("[MAIN] Sottoscrizione Calciobalilla locale attivata.");

            // 4. CONFIGURAZIONE CLOUD E AMMINISTRAZIONE EDGE
            MqttConnectOptions cloudOptions = new MqttConnectOptions();

            // --- LOGICA LWT: Notifica automatica dello stato OFFLINE in caso di disconnessione anomala ---
            String statusTopic = "bitpub/locali/" + idLocale + "/status";
            byte[] lastWillPayload = "{\"status\":\"OFFLINE\"}".getBytes();
            cloudOptions.setWill(statusTopic, lastWillPayload, 1, true);

            // --- SESSIONE DUREVOLE: Mantiene le sottoscrizioni attive sul broker cloud anche offline ---
            cloudOptions.setCleanSession(false);
            cloudOptions.setAutomaticReconnect(true);

            // Inizializzazione del client per la comunicazione verso il Cloud
            MqttClient cloudClient = new MqttClient(cloudBrokerUrl, "EdgeNode-Cloud-" + idLocale);
            cloudClient.connect(cloudOptions);
            System.out.println("[MAIN] Client Cloud connesso con successo (LWT e Sessione Durevole attivi).");

            // --- SOTTOSCRIZIONE COMANDI ADMIN: In ascolto per ordini remoti (es. FORCE_UNLOCK) ---
            // Il wildcard "+" permette di intercettare comandi per qualsiasi risorsa (tavolo) del locale
            cloudClient.subscribe("bitpub/locali/" + idLocale + "/biliardo/+/cmd", new AdminCommandListener());
            System.out.println("[MAIN] In ascolto per comandi di sblocco forzato remoti.");

            // --- AVVIO HEARTBEAT: Invio periodico segnale di presenza ONLINE ---
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(new HeartbeatTask(cloudClient, idLocale), 0, 60, TimeUnit.SECONDS);
            System.out.println("[MAIN] Sistema di Heartbeat avviato (frequenza: 60s).");

            // 5. AVVIO TASK DI INOLTRO: Svuotamento buffer verso il Cloud
            InoltroCloudTask inoltroTask = new InoltroCloudTask(bufferCondiviso, cloudClient);
            Thread inoltroThread = new Thread(inoltroTask);
            inoltroThread.start();
            System.out.println("[MAIN] Servizio di Inoltro Telemetria al Cloud avviato.");

        } catch (Exception e) {
            // Gestione errori critici che impediscono l'avvio del nodo
            System.err.println("[MAIN] Errore critico durante l'avvio dell'Edge Node:");
            e.printStackTrace();
        }
    }
}
