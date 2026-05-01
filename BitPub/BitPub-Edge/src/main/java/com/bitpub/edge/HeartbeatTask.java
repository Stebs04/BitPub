package com.bitpub.edge;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Task pianificato responsabile dell'invio periodico del segnale di presenza (Heartbeat).
 * Notifica al Cloud lo stato ONLINE del nodo locale per garantire il monitoraggio della rete.
 *
 * @author Stefano Bellan 20054330
 */
public class HeartbeatTask implements Runnable {

    /** Client MQTT utilizzato per la comunicazione con il broker. */
    private final IMqttClient mqttClient;

    /** Canale di pubblicazione specifico per lo stato del locale corrente. */
    private final String statusTopic;

    /**
     * Costruisce un nuovo task di heartbeat configurando il topic di destinazione.
     *
     * @param mqttClient L'istanza del client MQTT connessa.
     * @param venueId    L'identificativo univoco della sede (es: Milano-01).
     */
    public HeartbeatTask(IMqttClient mqttClient, String venueId) {
        this.mqttClient = mqttClient;
        // Il topic segue la specifica centralizzata: bitpub/locali/{id}/status
        this.statusTopic = "bitpub/locali/" + venueId + "/status";
    }

    /**
     * Esegue l'invio del messaggio di stato.
     * Viene invocato ciclicamente da un executor service lato Edge.
     */
    @Override
    public void run() {
        try {
            // Verifica preventiva dello stato della connessione prima del tentativo di invio
            if (mqttClient.isConnected()) {
                // Preparazione del payload JSON conforme alle attese del MqttAdminGateway del Cloud
                String payload = "{\"status\":\"ONLINE\"}";
                MqttMessage message = new MqttMessage(payload.getBytes());

                // QoS 1: Garantisce la consegna del ping al broker almeno una volta
                message.setQos(1);

                mqttClient.publish(statusTopic, message);
                System.out.println("[Edge] Segnale di presenza (ONLINE) inviato correttamente al Cloud.");
            }
        } catch (Exception e) {
            // Log dell'errore per la diagnostica locale in caso di fallimento della rete
            System.err.println("[Edge] Errore critico nell'invio dell'heartbeat: " + e.getMessage());
        }
    }
}
