package com.bitpub.Cloud;

import com.bitpub.buffer.MessageBuffer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Thread Consumatore: estrae i log dalla BlockingQueue e li invia al Cloud.
 * Implementa meccanismi di Resilienza Offline (Fallback) e QoS 1.
 *
 * @author Timothy (Fase 20 e 21: Architettura Offline Completa)
 * @author Stefano Bellan 20054330 (Implementazione di meccanismo di fallback e coda circolare)
 */
public class InoltroCloudTask implements Runnable {

    private final MessageBuffer buffer;
    private final MqttClient cloudClient;

    // 'volatile' garantisce che se Main ferma il thread, questo veda subito la
    // modifica
    private volatile boolean inEsecuzione;

    public InoltroCloudTask(MessageBuffer buffer, MqttClient cloudClient) {
        this.buffer = buffer;
        this.cloudClient = cloudClient;
        this.inEsecuzione = true;
    }

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
        System.out.println("[InoltroCloud] Thread Consumer avviato. In attesa di eventi...");

        while (inEsecuzione) {
            try {
                // Ispezione non distruttiva della testa della coda
                String payload = buffer.peek();

                if (payload == null) {
                    Thread.sleep(100); // Evita il busy-waiting eccessivo
                    continue;
                }

                if (cloudClient.isConnected()) {
                    MqttMessage message = new MqttMessage(payload.getBytes());
                    message.setQos(1); // Garantisce la consegna "at least once"

                    try {
                        String topicDestinazione = "bitpub/locali/sync";
                        cloudClient.publish(topicDestinazione, message);
                        
                        // Rimuovo dal buffer solo post-conferma successo
                        buffer.poll(); 
                        
                        System.out.println("[InoltroCloud] Inviato al Cloud con successo e rimosso dalla coda! Rimanenti: " + buffer.getDimensione());
                    } catch (MqttException e) {
                        // In caso di errore di pubblicazione, il messaggio resta nel buffer (logica di fallback)
                        System.err.println("[FALLBACK] Errore di invio al Broker. L'evento RESTA nella coda locale.");
                        Thread.sleep(2000);
                    }
                } else {
                    // Broker non raggiungibile: attendo il ripristino della connettività
                    System.out.println("[InoltroCloud] Connessione Cloud assente. L'evento RESTA in testa alla coda. Riprovo tra 5 secondi...");
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                // Gestione corretta dell'interruzione per lo shutdown del thread
                System.out.println("[InoltroCloud] Consumer interrotto. Chiusura in corso...");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}