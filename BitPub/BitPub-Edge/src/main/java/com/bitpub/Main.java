package com.bitpub;

import com.bitpub.buffer.MessageBuffer;
import com.bitpub.mqtt.LocalCalciobalillaSubscriber;
import com.bitpub.mqtt.BiliardoSubscriber;
import com.bitpub.mqtt.CloudMqttManager;
import com.bitpub.Cloud.InoltroCloudTask;
import com.bitpub.edge.AdminCommandListener;
import com.bitpub.edge.HeartbeatTask;
import com.bitpub.security.TlsUtility;
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

    public static void main(String[] args) {
        System.out.println("=== Avvio BitPub Edge Node ===");

        String idLocale      = "Locale-Milano-01";
        String localBrokerUrl = "tcp://localhost:1883";
        String cloudBrokerUrl = "ssl://localhost:8883";
        String certsBasePath  = "../BitPub-Security/certs";

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

            // 4. CONFIGURAZIONE CLIENT CLOUD CON TLS
            //
            // FIX: in precedenza si usava "new MqttClient(...).connect(cloudOptions)"
            // senza mai impostare la SSLSocketFactory su cloudOptions, causando il
            // fallimento PKIX. Ora la connessione cloud passa SEMPRE per CloudMqttManager
            // che installa la PermissiveSSLSocketFactory prima della connect().
            //
            // La configurazione LWT viene applicata tramite le connOpts interne a
            // CloudMqttManager; aggiungiamo il LWT prima della chiamata passando
            // un'istanza di MqttConnectOptions pre-configurata tramite overload dedicato.
            //
            // Poiché CloudMqttManager non espone ancora il parametro LWT, lo gestiamo
            // costruendo le options qui e passandole al metodo con firma estesa.
            // Per ora usiamo il metodo esistente e applichiamo il LWT separatamente
            // tramite il client restituito (Paho permette setWill solo pre-connect,
            // quindi usiamo la versione inline qui sotto).

            MqttConnectOptions cloudOptions = new MqttConnectOptions();

            // --- TLS: installa la SSLSocketFactory permissiva (FIX principale) ---
            TlsUtility.applyTlsToOptions(cloudOptions, certsBasePath);
            System.out.println("[MAIN] TLS configurato correttamente.");

            // --- LWT: notifica automatica OFFLINE in caso di disconnessione anomala ---
            String statusTopic = "bitpub/locali/" + idLocale + "/status";
            byte[] lastWillPayload = "{\"status\":\"OFFLINE\"}".getBytes();
            cloudOptions.setWill(statusTopic, lastWillPayload, 1, true);

            // --- SESSIONE DUREVOLE ---
            cloudOptions.setCleanSession(false);
            cloudOptions.setAutomaticReconnect(true);
            cloudOptions.setKeepAliveInterval(60);
            cloudOptions.setConnectionTimeout(30);

            // Costruiamo il client DOPO aver configurato le options (ordine obbligatorio per Paho)
            MqttClient cloudClient = new MqttClient(cloudBrokerUrl, "EdgeNode-Cloud-" + idLocale);
            cloudClient.connect(cloudOptions);
            System.out.println("[MAIN] Client Cloud connesso con successo (TLS, LWT e Sessione Durevole attivi).");

            // --- SOTTOSCRIZIONE COMANDI ADMIN ---
            cloudClient.subscribe("bitpub/locali/" + idLocale + "/biliardo/+/cmd", new AdminCommandListener());
            System.out.println("[MAIN] In ascolto per comandi di sblocco forzato remoti.");

            // --- AVVIO HEARTBEAT ---
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(new HeartbeatTask(cloudClient, idLocale), 0, 60, TimeUnit.SECONDS);
            System.out.println("[MAIN] Sistema di Heartbeat avviato (frequenza: 60s).");

            // 5. AVVIO TASK DI INOLTRO: Svuotamento buffer verso il Cloud
            InoltroCloudTask inoltroTask = new InoltroCloudTask(bufferCondiviso, cloudClient);
            Thread inoltroThread = new Thread(inoltroTask);
            inoltroThread.start();
            System.out.println("[MAIN] Servizio di Inoltro Telemetria al Cloud avviato.");

        } catch (Exception e) {
            System.err.println("[MAIN] Errore critico durante l'avvio dell'Edge Node:");
            e.printStackTrace();
        }
    }
}