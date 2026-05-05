package com.bitpub.mqtt;

import com.bitpub.buffer.MessageBuffer;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.bitpub.utils.BiliardoTopicConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscriber per MQTT e simulatore Biliardo
 * @author Stefano Bellan 20054330
 * @modified Stefano Bellan 20054330 - Fase 25: aggiunto logging professionale
 */
public class BiliardoSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(BiliardoSubscriber.class);

    private final MqttClient mqttClient;
    private final MessageBuffer buffer;

    /**
     * Setta il client MQTT per il thread Subscriber Biliardo
     * @param client MqttClient in configurazione locale su localhost.
     * @param buffer MessageBuffer associato al modulo Edge.
     */
    public BiliardoSubscriber(MqttClient client, MessageBuffer buffer) {
        this.mqttClient = client;
        this.buffer = buffer;
    }

    /**
     * Sottoscrizione al topic del locale per Biliardo tramite subscriber mqtt.
     */
    public void iscrivitiTopicBiliardo() {
        try {
            mqttClient.subscribe(BiliardoTopicConstants.TOPIC_IMBUCATE, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    
                    try {
                        // Strict Filter: Process ONLY `source=DEVICE` with valid hardware signatures.
                        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();
                        if (!json.has("source") || !"DEVICE".equals(json.get("source").getAsString()) || !json.has("hardwareSignature") || json.get("hardwareSignature").getAsString().isEmpty()) {
                            logger.warn("Evento scartato (no hardware validation): {}", payload);
                            return;
                        }

                        logger.debug("Evento ricevuto e validato - tipo: Biliardo, sorgente: {}, timestamp: {}", topic, System.currentTimeMillis());
                        
                        // Inserisce il messaggio nel buffer condiviso (thread-safe tramite LinkedBlockingQueue)
                        buffer.push(payload);
                        
                        logger.info("Evento elaborato con successo - id: (JSON Biliardo) {}", payload);
                    } catch(Exception ex) {
                        logger.warn("Evento ignorato - formato non valido: {}", payload);
                        logger.error("Errore critico nella ricezione evento", ex);
                    }
                }
            });
            logger.info("Sottoscrizione avvenuta ai biliardo per topic {}", BiliardoTopicConstants.TOPIC_IMBUCATE);
        } catch (Exception e) {
            logger.error("Errore critico nella ricezione evento su iscrizione del topic:", e);
        }
    }
}
