package com.bitpub.sync;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gestisce il ciclo di vita della sessione MQTT e fornisce indicatori di backpressure.
 * Traccia la disponibilità del tunnel verso il Cloud.
 */
public class MqttSessionManager implements MqttCallbackExtended {

    private static final Logger logger = LoggerFactory.getLogger(MqttSessionManager.class);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        isConnected.set(true);
        if (reconnect) {
            logger.info("[SESSION MANAGER] Riconnessione MQTT automatica completata verso: {}", serverURI);
        } else {
            logger.info("[SESSION MANAGER] Prima connessione MQTT stabilita: {}", serverURI);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        isConnected.set(false);
        logger.warn("[SESSION MANAGER] Tunnel MQTT interrotto. Sospensione invio pacchetti. Causa: {}", cause != null ? cause.getMessage() : "Sconosciuta");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // Gestito da EdgeMqttClient o altri subscriber
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Acknowledgement tracciato in modo asincrono, ma noi lo facciamo in modo sincrono nel SyncManager per maggiore coerenza
    }

    /**
     * @return true se il client è operativo e in grado di ricevere traffico
     */
    public boolean isSessionActive() {
        return isConnected.get();
    }
}
