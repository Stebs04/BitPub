package it.bitpub.edge.cloud;

import it.bitpub.edge.buffer.BufferEventi;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Thread dedicato all'estrazione dei log dal buffer locale
 * e all'invio verso il server Cloud MQTT.
 * Implementa il paradigma Store and Forward per la resilienza offline.
 */
public class InoltroCloudTask implements Runnable {

    private BufferEventi buffer;
    private MqttClient cloudClient;
    private boolean inEsecuzione;

    public InoltroCloudTask(BufferEventi buffer, MqttClient cloudClient) {
        this.buffer = buffer;
        this.cloudClient = cloudClient;
        this.inEsecuzione = true;
    }

    public void fermaInoltro() {
        this.inEsecuzione = false;
    }

    @Override
    public void run() {
        System.out.println("[InoltroCloud] Thread di inoltro verso il Cloud avviato.");

        while (inEsecuzione) {
            try {
                // 1. Controlliamo se ci sono messaggi da spedire
                if (buffer.getDimensione() > 0) {

                    // 2. Verifichiamo se abbiamo connessione internet verso il Cloud
                    if (cloudClient.isConnected()) {

                        // Estraiamo in modo thread-safe (synchronized) dal buffer
                        String payload = buffer.prelevaEvento();

                        if (payload != null) {
                            MqttMessage message = new MqttMessage(payload.getBytes());
                            message.setQos(1); // QoS 1: Garantisce la consegna con ricevuta di ritorno

                            // Pubblicazione verso il Cloud (qui usiamo un topic generico di arrivo)
                            // Nota: In un caso reale, il Cloud ricevitore si iscriverà a "bitpub/locali/#"
                            String topicDestinazione = "bitpub/locali/sync";

                            cloudClient.publish(topicDestinazione, message);
                            System.out.println("[InoltroCloud] Messaggio inviato al Cloud! Rimanenti: " + buffer.getDimensione());
                        }
                    } else {
                        // Niente internet! Aspettiamo senza fare estrazioni dal buffer (Store and Forward)
                        System.out.println("[InoltroCloud] Connessione assente. Accumulo in corso... Coda: " + buffer.getDimensione());
                        Thread.sleep(5000); // Riprova dopo 5 secondi
                    }
                } else {
                    // Buffer vuoto, dormiamo un po' per non consumare CPU
                    Thread.sleep(1000);
                }

            } catch (MqttException e) {
                System.err.println("[InoltroCloud] Errore di pubblicazione (QoS 1): " + e.getMessage());
                // In caso di errore durante l'invio (es. caduta rete improvvisa), Paho MQTT
                // con setCleanSession(false) manterrà il messaggio in-flight, quindi non lo perdiamo.
            } catch (InterruptedException e) {
                System.err.println("[InoltroCloud] Thread interrotto.");
                e.printStackTrace();
            }
        }
    }
}