package com.bitpub.Cloud;

import com.bitpub.buffer.MessageBuffer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread Consumatore: estrae i log dalla BlockingQueue e li invia al Cloud.
 * Implementa meccanismi di Resilienza Offline (Fallback) e QoS 1.
 *
 * @author Timothy (Fase 20 e 21: Architettura Offline Completa)
 * @author Stefano Bellan 20054330 (Implementazione di meccanismo di fallback e coda circolare)
 * @modified Stefano Bellan 20054330 - Fase 25: aggiunto logging professionale
 */
public class InoltroCloudTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(InoltroCloudTask.class);

    private final MessageBuffer buffer;
    private final MqttClient cloudClient;

    // 'volatile' garantisce che se Main ferma il thread, questo veda subito la
    // modifica
    private volatile boolean inEsecuzione;

    private boolean wasConnected = true;
    private int attempt = 0;
    private long downtimeStart = 0;
    private final int MAX_ATTEMPTS = 50; // Soglia 50 tentativi per log error critico
    private int eventiInviati = 0; // Contatore batch per flush logging

    /**
     * Inizializza il task in thread separato verso il cloud remoto
     * @param buffer riferimento buffer edge
     * @param cloudClient client remoto nel Cloud
     */
    public InoltroCloudTask(MessageBuffer buffer, MqttClient cloudClient) {
        this.buffer = buffer;
        this.cloudClient = cloudClient;
        this.inEsecuzione = true;
    }

    /**
     * Arresta la lettura pendente e blocca loop
     */
    public void fermaInoltro() {
        this.inEsecuzione = false;
    }

    /**
     * Ciclo principale di esecuzione del thread Consumer.
     * <p>
     * Il metodo monitora costantemente un buffer locale e tenta di inoltrare i messaggi 
     * a un broker MQTT remoto. Implementa una logica di persistenza: il messaggio viene 
     * rimosso dalla coda solo dopo la conferma di avvenuta pubblicazione (ACK).
     * </p>
     * 
     * Gestisce i seguenti scenari:
     * <ul>
     *   <li>Buffer vuoto: attesa attiva (polling) di 100ms.</li>
     *   <li>Connessione assente: retry ogni 5 secondi senza perdita di dati.</li>
     *   <li>Errore MQTT: attesa di 2 secondi prima del prossimo tentativo.</li>
     * </ul>
     */
    @Override
    public void run() {
        logger.info("Connessione Edge?Cloud stabilita - endpoint: {}", cloudClient.getServerURI());

        while (inEsecuzione) {
            try {
                // Ispezione non distruttiva della testa della coda
                String payload = buffer.peek();

                if (payload == null) {
                    if (eventiInviati > 0) {
                        logger.info("Flush buffer completato - {} eventi inviati al Cloud", eventiInviati);
                        eventiInviati = 0; // reset
                    }
                    Thread.sleep(100); // Evita il busy-waiting eccessivo
                    continue;
                }

                if (cloudClient.isConnected()) {
                    if (!wasConnected) {
                        long downtimeMs = System.currentTimeMillis() - downtimeStart;
                        logger.info("Riconnessione avvenuta con successo - endpoint: {}, dopo {} ms di interruzione", cloudClient.getServerURI(), downtimeMs);
                        wasConnected = true;
                        attempt = 0;
                    }

                    MqttMessage message = new MqttMessage(payload.getBytes());
                    message.setQos(1); // Garantisce la consegna "at least once"

                    try {
                        String topicDestinazione = "bitpub/locali/sync";
                        cloudClient.publish(topicDestinazione, message);
                        
                        // Rimuovo dal buffer solo post-conferma successo
                        buffer.poll(); 
                        eventiInviati++;
                        
                        logger.debug("Log inviato al cloud! Rimanenti {}", buffer.getDimensione());
                    } catch (MqttException e) {
                        // In caso di errore di pubblicazione, il messaggio resta nel buffer (logica di fallback)
                        logger.warn("Errore di invio al Broker. L'evento RESTA nella coda locale.", e);
                        Thread.sleep(2000);
                    }
                } else {
                    if (wasConnected) {
                        logger.warn("Connessione persa verso il Cloud - endpoint: {}, tentativo riconnessione in {} ms", cloudClient.getServerURI(), 5000);
                        wasConnected = false;
                        downtimeStart = System.currentTimeMillis();
                        attempt = 1;
                    } else {
                        attempt++;
                        logger.info("Tentativo di riconnessione #{} verso {}", attempt, cloudClient.getServerURI());
                        if (attempt >= MAX_ATTEMPTS) {
                            logger.error("Impossibile riconnettersi al Cloud dopo {} tentativi - endpoint: {}", MAX_ATTEMPTS, cloudClient.getServerURI());
                            attempt = 0; // Reset after logging error
                        }
                    }

                    // Broker non raggiungibile: attendo il ripristino della connettivita'
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                // Gestione corretta dell'interruzione per lo shutdown del thread
                logger.warn("Consumer interrotto. Chiusura in corso...");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
