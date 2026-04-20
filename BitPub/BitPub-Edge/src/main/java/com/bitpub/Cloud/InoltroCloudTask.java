package com.bitpub.Cloud;

import com.bitpub.buffer.MessageBuffer;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Thread dedicato all'estrazione dei log dal buffer locale
 * e all'invio verso il server Cloud MQTT.
 * Implementa il paradigma Store and Forward per la resilienza offline.
 */
public class InoltroCloudTask implements Runnable {

    private final MessageBuffer buffer;
    private final MqttClient cloudClient;
    private boolean inEsecuzione;

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
        System.out.println("[InoltroCloud] Thread di inoltro verso il Cloud avviato.");

        while (inEsecuzione) {
            try {
                if (buffer.getDimensione() > 0) {

                    if (cloudClient.isConnected()) {

                        // pop() è thread-safe (synchronized) in MessageBuffer
                        String payload = buffer.pop();

                        if (payload != null) {
                            MqttMessage message = new MqttMessage(payload.getBytes());
                            message.setQos(1);

                            String topicDestinazione = "bitpub/locali/sync";
                            cloudClient.publish(topicDestinazione, message);
                            System.out.println("[InoltroCloud] Messaggio inviato al Cloud! Rimanenti: " + buffer.getDimensione());
                        }
                    } else {
                        System.out.println("[InoltroCloud] Connessione assente. Accumulo in corso... Coda: " + buffer.getDimensione());
                        Thread.sleep(5000);
                    }
                } else {
                    Thread.sleep(1000);
                }

            } catch (MqttException e) {
                System.err.println("[InoltroCloud] Errore di pubblicazione (QoS 1): " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("[InoltroCloud] Thread interrotto.");
                e.printStackTrace();
            }
        }
    }
}