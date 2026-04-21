package com.bitpub.Cloud;

import com.bitpub.buffer.MessageBuffer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Thread Consumatore: estrae i log dalla BlockingQueue e li invia al Cloud.
 */
public class InoltroCloudTask implements Runnable {

    private final MessageBuffer buffer;
    private final MqttClient cloudClient;

    // 'volatile' assicura che se un altro thread chiama fermaInoltro(),
    // questo thread veda subito la modifica senza problemi di cache della CPU.
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
            try {
                // 1. ESTRAZIONE BLOCCANTE (La magia della BlockingQueue)
                // Se non ci sono messaggi, il thread si ferma qui. Niente più if(dimensione > 0)!
                String payload = buffer.take();

                // 2. INVIO AL CLOUD
                if (cloudClient.isConnected()) {
                    MqttMessage message = new MqttMessage(payload.getBytes());
                    message.setQos(1); // Garantisce che il messaggio arrivi (Store and Forward di MQTT)

                    String topicDestinazione = "bitpub/locali/sync";
                    cloudClient.publish(topicDestinazione, message);

                    System.out.println("[InoltroCloud] Inoltrato al Cloud. Rimanenti: " + buffer.getDimensione());
                } else {
                    // 3. GESTIONE OFFLINE (Store and Forward manuale)
                    System.out.println("[InoltroCloud] Connessione Cloud assente. Rimetto in coda.");
                    // Siccome l'abbiamo prelevato con take(), se non c'è rete lo rimettiamo dentro
                    buffer.push(payload);

                    // Aspettiamo un po' prima di riprovare per non intasare il sistema
                    Thread.sleep(5000);
                }

            } catch (InterruptedException e) {
                System.out.println("[InoltroCloud] Consumer interrotto. Chiusura in corso...");
                // Buona pratica: ripristinare lo stato di interruzione del thread
                Thread.currentThread().interrupt();
                break; // Usciamo dal ciclo while
            } catch (MqttException e) {
                System.err.println("[InoltroCloud] Errore di pubblicazione MQTT: " + e.getMessage());
            }
        }
    }
}