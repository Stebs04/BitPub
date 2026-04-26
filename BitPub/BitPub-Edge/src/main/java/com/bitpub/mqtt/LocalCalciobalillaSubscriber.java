package com.bitpub.mqtt;

import com.bitpub.buffer.MessageBuffer;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;

/**
 * Gestisce la sottoscrizione ai messaggi MQTT relativi agli eventi dei calciobalilla.
 * Implementa {@link MqttCallback} per reagire asincronamente agli eventi del broker.
 *
 * @author Stefano Bellan 20054330
 * @modified Stefano Bellan 20054330 - Fase 25: aggiunto logging professionale
 */
public class LocalCalciobalillaSubscriber implements MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(LocalCalciobalillaSubscriber.class);

    private final MessageBuffer messageBuffer;
    private final MqttClient mqttClient;

    /**
     * Inizializza il subscriber con il buffer di destinazione e le credenziali del broker.
     *
     * @param messageBuffer Coda di destinazione per i messaggi ricevuti.
     * @param brokerUrl     Indirizzo del broker (es. tcp://localhost:1883).
     * @param clientId      Identificativo univoco del client.
     * @throws MqttException Se l'inizializzazione del client fallisce.
     */
    public LocalCalciobalillaSubscriber(MessageBuffer messageBuffer, String brokerUrl, String clientId) throws MqttException {
        this.messageBuffer = messageBuffer;
        // Inizializzazione client con persistenza di default (memory)
        this.mqttClient = new MqttClient(brokerUrl, clientId);
    }

    /**
     * Callback invocata all'arrivo di un nuovo messaggio sui topic sottoscritti.
     *
     * @param topic   Topic in cui l'evento occorre
     * @param message Messaggio mqtt che incapsula la stringa Json inviata
     * @throws Exception lancia eccezione nel caso generico 
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // Conversione payload garantendo il charset UTF-8 per evitare problemi cross-platform
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        try{
            logger.debug("Evento ricevuto - tipo: Calciobalilla, sorgente: {}, timestamp: {}", topic, System.currentTimeMillis());
            // Inserimento nel buffer per l'elaborazione disaccoppiata (produttore-consumatore)
            messageBuffer.push(payload);
            logger.info("Evento elaborato con successo - id: (JSON)", payload);
        } catch(Exception e) {
            logger.warn("Evento ignorato - formato non valido: {}", payload);
            logger.error("Errore critico nella ricezione evento", e);
        }
    }

    /**
     * Gestisce l'interruzione imprevista della connessione con il broker.
     * @param cause Causa persa connessione
     */
    @Override
    public void connectionLost(Throwable cause) {
        logger.error("Connessione MQTT persa", cause);
    }

    /**
     * Invocata quando un messaggio inviato (se presente) è stato consegnato correttamente.
     * @param token Identificativo delivery
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Metodo non utilizzato in quanto il subscriber riceve soltanto
    }

    /**
     * Configura il client, stabilisce la connessione e attiva la sottoscrizione ai topic.
     * Utilizza wildcard per intercettare eventi da diversi locali/tavoli.
     *
     * @throws MqttException Se la connessione o la sottoscrizione falliscono.
     */
    public void start() throws MqttException {
        mqttClient.setCallback(this);
        mqttClient.connect();

        // Sottoscrizione gerarchica: bitpub/locali/[ID_LOCALE]/calciobalilla/[ID_TAVOLO]/eventi
        mqttClient.subscribe("bitpub/locali/+/calciobalilla/+/eventi");

        logger.info("Connessione stabilita sui simulatori calciobalilla !!");
    }
}
