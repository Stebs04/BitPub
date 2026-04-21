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
 */
public class InoltroCloudTask implements Runnable {

    private final MessageBuffer buffer;
    private final MqttClient cloudClient;

    // 'volatile' garantisce che se Main ferma il thread, questo veda subito la modifica
    private volatile boolean inEsecuzione;

    public InoltroCloudTask(MessageBuffer buffer, MqttClient cloudClient) {
        this.buffer = buffer;
        this.cloudClient = cloudClient;
        this.inEsecuzione = true;
    }

    public void fermaInoltro() {
        this.inEsecuzione = false;
    }

    @Override
    public void run() {
        System.out.println("[InoltroCloud] Thread Consumer avviato. In attesa di eventi...");

        while (inEsecuzione) {
            // Dichiariamo payload qui fuori così possiamo recuperarlo se qualcosa va storto
            String payload = null;

            try {
                // 1. ESTRAZIONE BLOCCANTE (Fase 20)
                // Il thread si mette in pausa da solo finché i simulatori non inseriscono un dato
                payload = buffer.take();

                // 2. CONTROLLO CONNESSIONE E INVIO (Fase 21)
                if (cloudClient.isConnected()) {
                    MqttMessage message = new MqttMessage(payload.getBytes());

                    // Impostiamo QoS 1: vogliamo la conferma dal Broker (Store and Forward MQTT)
                    message.setQos(1);

                    try {
                        String topicDestinazione = "bitpub/locali/sync";
                        cloudClient.publish(topicDestinazione, message);
                        System.out.println("[InoltroCloud] Inviato al Cloud con successo! Rimanenti: " + buffer.getDimensione());
                    } catch (MqttException e) {
                        // 3. MECCANISMO DI FALLBACK (Stefano / Timothy)
                        // Se il publish fallisce per un micro-distacco di rete, salviamo l'evento!
                        System.err.println("[FALLBACK] Errore di invio al Broker. Ripristino l'evento nel buffer locale.");
                        buffer.push(payload);

                        // Piccola pausa per non impazzire riprovando all'infinito
                        Thread.sleep(2000);
                    }
                } else {
                    // 4. GESTIONE OFFLINE PURA
                    System.out.println("[InoltroCloud] Connessione Cloud assente. Rimetto in coda. Buffer: " + (buffer.getDimensione() + 1));

                    // Siccome abbiamo "tirato fuori" il dato con take(), dobbiamo rimetterlo dentro
                    buffer.push(payload);

                    // Aspettiamo 5 secondi per dare tempo al Client MQTT di ricollegarsi automaticamente
                    Thread.sleep(5000);
                }

            } catch (InterruptedException e) {
                System.out.println("[InoltroCloud] Consumer interrotto. Chiusura in corso...");
                // Ripristiniamo lo stato di interruzione del thread (è una buona pratica Java)
                Thread.currentThread().interrupt();
                break; // Usciamo dal ciclo while e fermiamo il thread
            }
        }
    }
}