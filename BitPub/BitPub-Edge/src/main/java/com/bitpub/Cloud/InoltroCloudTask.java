package com.bitpub.Cloud;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bitpub.buffer.BufferDatiEdge;

/**
 * Worker thread preposto all'estrazione e all'esportazione dei pacchetti telemetrici
 * accumulati localmente. Rappresenta il consumatore primario all'interno dell'architettura
 * Store-and-Forward implementata sull'Edge Node.
 * Esegue un blocco attivo (blocking wait) sulle code in assenza di traffico per ottimizzare i cicli CPU,
 * applica policy di ritenzione incondizionata del payload durante i black-out di rete e
 * si affida intrinsecamente all'handshake crittografico TLS offerto dal broker Cloud.
 * @author Timothy (Fase 20 e 21: Architettura Offline Completa)
 * @author Stefano Bellan 20054330
 */
public class InoltroCloudTask implements Runnable {

    // Motore di diagnostica configurato col pattern standard di SLF4J
    private static final Logger logger = LoggerFactory.getLogger(InoltroCloudTask.class);

    // Struttura a coda thread-safe (LinkedBlockingQueue) incaricata di accumulare i task
    private final BufferDatiEdge buffer;

    // Istanza di controllo del tunnel socket verso l'infrastruttura centrale
    private final MqttClient cloudClient;

    /**
     * Costruttore d'iniezione. Accoppia il consumatore al buffer sorgente e al client di trasporto.
     *
     * @param buffer Il contenitore thread-safe in cui i simulatori hanno accatastato gli eventi fisici
     * @param cloudClient Il bridge Paho connesso e autenticato verso il cloud
     */
    public InoltroCloudTask(BufferDatiEdge buffer, MqttClient cloudClient) {
        this.buffer = buffer;
        this.cloudClient = cloudClient;
    }

    /**
     * Ciclo di vita infinito eseguito in background.
     * Implementa la pipeline logica di Store-and-Forward: preleva atomica del messaggio,
     * conversione in stream di byte e accodamento sincrono sul layer TCP/IP.
     * Qualsiasi fallimento infrastrutturale innesca un loop secondario di backoff esponenziale
     * o lineare che impedisce al Worker di prelevare elementi successivi finché l'attuale
     * non viene certificato come recapitato.
     */
    @Override
    public void run() {
        logger.info("[INOLTRO CLOUD] Task di Store-and-Forward avviato. In attesa di log...");

        // Iterazione condizionata dallo stato di esecuzione del thread per supportare il graceful shutdown
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Sospensione del thread sul blocco del buffer in assenza di messaggi (Zero CPU Burn).
                // Il wakeup viene triggerato nativamente dall'add operato sul buffer dai moduli produttori.
                String payload = buffer.take();

                boolean messaggioConsegnato = false;

                // Loop secondario di garanzia del recapito: l'avanzamento al payload successivo
                // è strettamente subordinato alla ricezione del PUBACK da parte di Mosquitto.
                while (!messaggioConsegnato && !Thread.currentThread().isInterrupted()) {

                    if (cloudClient != null && cloudClient.isConnected()) {
                        try {
                            // Allocazione del frame MQTT incapsulando il JSON e forzando l'encoding UTF-8
                            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

                            // Imposizione del livello di servizio QoS 1. Demandando alla libreria Paho
                            // la gestione dei retry intrinseci per compensare latenze passeggere
                            message.setQos(1);

                            // Inoltro bloccante: il comando attende formalmente l'Acknowledgement (PUBACK)
                            // dal broker per certificare la persistenza del messaggio
                            cloudClient.publish("bitpub/locali/sync", message);

                            // Sblocco del flag di guardia: il while termina e si passa al buffer.take() successivo
                            messaggioConsegnato = true;
                            logger.debug("[INOLTRO CLOUD] Evento consegnato con successo (ACK ricevuto).");

                        } catch (MqttException e) {
                            // Fail-over di livello 1: il broker non ha confermato o il socket è imploso.
                            // Si induce una micro-sospensione (Sleep) per allentare lo stress sul layer di rete
                            // prima di ritentare la pubblicazione dello stesso payload.
                            logger.warn("[INOLTRO CLOUD] Errore di invio (MqttException). Riprovo tra 5 secondi. Causa: {}", e.getMessage());
                            Thread.sleep(5000);
                        }
                    } else {
                        // Fail-over di livello 2: sconnessione evidente. Trattenuta del dato in memoria ram
                        // e polling differito per attendere il ristabilimento fisiologico del tunnel
                        logger.debug("[INOLTRO CLOUD] Client disconnesso dal Cloud. Trattengo il messaggio e attendo...");
                        Thread.sleep(5000);
                    }
                }

            } catch (InterruptedException e) {
                // Intercettazione del segnale di shut-down o di kill della VM.
                logger.warn("[INOLTRO CLOUD] Task interrotto. Spegnimento in corso.");

                // Ricostituzione esplicita del flag interrupt per propagare la segnalazione
                // ai frame stack di livello superiore, consentendo una chiusura incontaminata del worker
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // Blocco di difesa finale contro anomalie di conversione cast/encoding impreviste
                logger.error("[INOLTRO CLOUD] Errore critico nel ciclo di inoltro: {}", e.getMessage());
            }
        }
    }
}