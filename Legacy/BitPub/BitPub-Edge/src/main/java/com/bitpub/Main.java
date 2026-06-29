package com.bitpub;

import com.bitpub.buffer.PersistentEventStore;
import com.bitpub.sync.MqttSessionManager;
import com.bitpub.sync.ReplayManager;
import com.bitpub.sync.SyncManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bitpub.edge.EdgeMqttClient;
import com.bitpub.edge.GameTableStateManager;
import com.bitpub.edge.HeartbeatTask;
import com.bitpub.edge.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;

/**
 * Punto di ingresso e centro nevralgico dell'infrastruttura del nodo BitPub Edge.
 * L'architettura è stata progettata per fungere da layer di disaccoppiamento tra il mondo IoT 
 * (sensori dei biliardi/calciobalilla) e il cloud centrale, offrendo funzionalità 
 * di elaborazione locale, gestione del buffer e telemetria.
 * Questa classe orchestra il ciclo di vita dell'applicazione, dall'inizializzazione dei motori
 * asincroni alla registrazione degli hook per garantire un arresto aggraziato del sistema.
 *
 * @author Stefano Bellan 20054330
 */
public class Main {

    // Istanza del logger SLF4J dedicata al tracciamento delle fasi di bootstrap e teardown del nodo
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Metodo di innesco primario dell'applicazione Edge.
     * Alloca la memoria per le strutture a coda (buffer), inizializza le connessioni persistenti 
     * e orchestra i thread in background per la manutenzione dello stato.
     *
     * @param args Parametri iniettati dall'interprete di riga di comando
     */
    public static void main(String[] args) {
        logger.info("Avvio BitPub Edge Node in corso...");

        try {
            // Fase 1: Predisposizione dell'architettura persistente
            PersistentEventStore buffer = new PersistentEventStore("edge_events.db");
            ReplayManager replayManager = new ReplayManager(buffer);
            replayManager.recoverFromCrash();

            GameTableStateManager stateManager = new GameTableStateManager();
            MqttSessionManager sessionManager = new MqttSessionManager();

            SSLContext sslContext = SslContextFactory.build(
                    "../BitPub-Security/certs/ca.crt",
                    "../BitPub-Security/certs/client.crt",
                    "../BitPub-Security/certs/client_pkcs8.key"
            );

            // Fase 2: Bootstrapping della messaggistica locale e sinc
            EdgeMqttClient mqttClient = new EdgeMqttClient(stateManager, buffer, sslContext, sessionManager);
            mqttClient.connect();

            SyncManager syncManager = new SyncManager(buffer, mqttClient.getClient(), sessionManager);
            syncManager.start();

            // Fase 3: Orchestrazione del battito cardiaco (Heartbeat)
            // Utilizzo di un executor pre-dimensionato per isolare la telemetria periodica
            // evitando interferenze col thread di ascolto degli eventi fisici.
            ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
            
            // Programmazione del segnale di keep-alive per notificare al cloud la vitalità dell'hub
            heartbeatScheduler.scheduleAtFixedRate(
                new HeartbeatTask(mqttClient.getClient(), "Locale_Esempio_01"),
                0, 15, TimeUnit.SECONDS
            );

            logger.info("Sistema Edge operativo. Heartbeat avviato ogni 15s.");

            // Fase 4: Definizione della logica di Graceful Shutdown
            // Registrazione di un listener a livello di Virtual Machine per intercettare 
            // le direttive di interruzione del sistema operativo (es. CTRL+C, SIGTERM).
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.warn("Rilevato segnale di spegnimento (SIGTERM/Interrupt).");
                try {
                    // Abbattimento controllato del demone temporale con timeout di salvaguardia
                    heartbeatScheduler.shutdown();
                    if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        heartbeatScheduler.shutdownNow();
                    }

                    // Chiusura del worker di sincronizzazione offline
                    if (syncManager != null) {
                        syncManager.stop();
                    }

                    // Chiusura aggraziata del DB MapDB
                    if (buffer != null) {
                        buffer.close();
                    }

                    // Chiusura formale del socket MQTT prima della distruzione del processo
                    // per inibire l'attivazione del testamento (LWT) lato broker.
                    if (mqttClient != null) {
                        mqttClient.disconnect();
                        logger.info("Client MQTT disconnesso correttamente.");
                    }
                    
                    logger.info("Chiusura del nodo Edge completata con successo.");
                } catch (Exception ex) {
                    logger.error("Errore durante la procedura di spegnimento: {}", ex.getMessage());
                }
            }));

        } catch (Exception e) {
            // Terminazione d'emergenza a fronte di criticità non gestibili in fase di setup
            logger.error("Errore critico durante l'avvio: {}", e.getMessage());
            System.exit(1);
        }
    }
}