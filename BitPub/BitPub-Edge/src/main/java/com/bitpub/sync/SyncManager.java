package com.bitpub.sync;

import com.bitpub.buffer.PersistentEventStore;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestratore principale della sincronizzazione offline.
 * Estrae ciclicamente gli eventi dalla Persistent Queue e cerca di recapitarli al Cloud.
 * In caso di fallimento, applica l'algoritmo di Backoff senza scartare il dato.
 */
public class SyncManager {

    private static final Logger logger = LoggerFactory.getLogger(SyncManager.class);

    private final PersistentEventStore eventStore;
    private final IMqttClient mqttClient;
    private final MqttSessionManager sessionManager;
    private final RetryScheduler retryScheduler;
    private final ExecutorService executor;

    public SyncManager(PersistentEventStore eventStore, IMqttClient mqttClient, MqttSessionManager sessionManager) {
        this.eventStore = eventStore;
        this.mqttClient = mqttClient;
        this.sessionManager = sessionManager;
        this.retryScheduler = new RetryScheduler(2000, 60000, 2.0); // 2s, max 60s, x2
        this.executor = Executors.newSingleThreadExecutor(); // Mantiene l'ordinamento FIFO (un solo thread)
    }

    public void start() {
        executor.submit(() -> {
            logger.info("[SYNC MANAGER] Avviato worker di sincronizzazione offline.");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Backpressure passiva: attende se MQTT è palesemente disconnesso
                    if (!sessionManager.isSessionActive() && !mqttClient.isConnected()) {
                        Thread.sleep(2000);
                        continue;
                    }

                    // Bloccante: non consuma CPU se non ci sono eventi
                    Map.Entry<Long, String> entry = eventStore.takeNext();
                    Long seqId = entry.getKey();
                    String payload = entry.getValue();

                    boolean delivered = false;

                    while (!delivered && !Thread.currentThread().isInterrupted()) {
                        try {
                            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                            message.setQos(1); // At least once

                            // Tentativo di invio. La chiamata è sincrona e attende PUBACK
                            mqttClient.publish("bitpub/locali/sync", message);

                            // Successo
                            delivered = true;
                            retryScheduler.reset();
                            eventStore.acknowledge(seqId);
                            logger.debug("[SYNC MANAGER] Consegna confermata per Seq={}", seqId);

                        } catch (MqttException e) {
                            logger.warn("[SYNC MANAGER] Fallimento recapito per Seq={}. Causa: {}. Applico Backoff.", seqId, e.getMessage());
                            retryScheduler.sleepWithBackoff();
                        }
                    }

                } catch (InterruptedException e) {
                    logger.warn("[SYNC MANAGER] Thread interrotto. Spegnimento worker.");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("[SYNC MANAGER] Errore critico nel processing della coda", e);
                }
            }
        });
    }

    public void stop() {
        executor.shutdownNow();
    }
}
