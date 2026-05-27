package com.bitpub.edge;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Task operativo progettato per la trasmissione ciclica della telemetria di base (Heartbeat)
 * verso l'infrastruttura Cloud.
 */
public class HeartbeatTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatTask.class);
    private final IMqttClient mqttClient;
    private final String statusTopic;

    public HeartbeatTask(IMqttClient mqttClient, String venueId) {
        this.mqttClient = mqttClient;
        this.statusTopic = "bitpub/locali/" + venueId + "/status";
    }

    @Override
    public void run() {
        try {
            // Aggiunto controllo di sicurezza: verifica prima che mqttClient non sia NULL
            if (mqttClient != null && mqttClient.isConnected()) {

                String payload = "{\"status\":\"ONLINE\"}";
                MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                message.setQos(1);

                try {
                    mqttClient.publish(statusTopic, message);
                    logger.debug("[HEARTBEAT] Segnale ONLINE inviato correttamente su {}", statusTopic);
                } catch (MqttException me) {
                    logger.warn("[HEARTBEAT] Cloud non raggiungibile (MqttException). Riprovo al prossimo ciclo. Errore: {}", me.getMessage());
                }

            } else {
                logger.debug("[HEARTBEAT] Invio saltato: client MQTT attualmente disconnesso o non inizializzato.");
            }
        } catch (Exception e) {
            logger.error("[HEARTBEAT] Errore critico imprevisto nel task: {}", e.getMessage());
        }
    }
}