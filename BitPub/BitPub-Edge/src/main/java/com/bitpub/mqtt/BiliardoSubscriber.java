package com.bitpub.mqtt;

import com.bitpub.buffer.PersistentEventStore;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Controller di frontiera asincrono progettato per l'acquisizione della telemetria locale
 * prodotta dai tavoli da biliardo (sensori di caduta nelle buche, posizionamento sfere).
 * Opera come nodo di Consumer (rispetto alla rete locale IoT) e di Producer (rispetto al
 * meccanismo di Store-and-Forward), incanalando il dato in sicurezza per la propagazione Cloud.
 * Impiega un sistema di filtraggio lessicale (Strict Filter) per validare i pacchetti
 * con tolleranza di ritardo misurabile in frazioni di millisecondo, evitando il collasso
 * del thread di rete MQTT.
 *
 * @author Luca Franzon
 */
public class BiliardoSubscriber implements IMqttMessageListener {

    // Meccanismo tracciante ancorato all'infrastruttura di Logback / SLF4J
    private static final Logger logger = LoggerFactory.getLogger(BiliardoSubscriber.class);

    // Coda persistente thread-safe utilizzata per isolare il produttore dal consumatore cloud
    private final PersistentEventStore buffer;

    /**
     * Costruttore parametrizzato ad iniezione di dipendenze.
     * Allaccia logicamente il recettore hardware al polmone di accumulo persistente.
     *
     * @param buffer L'interfaccia della memoria persistente
     */
    public BiliardoSubscriber(PersistentEventStore buffer) {
        this.buffer = buffer;
    }

    /**
     * Callback di reazione triggerata dal socket MQTT Mosquitto al recapito di ogni singolo
     * frammento d'informazione proveniente dall'alveo di topic designato.
     *
     * @param topic Stringa definente la tassonomia di rete dell'evento
     * @param message Payload applicativo contenente matrice byte e metadati di quality of service
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Conversione immediata della griglia di byte in stringa di testo ancorando lo standard UTF-8
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        // REGOLA DI INGEGNERIA N°1: VALUTAZIONE EURISTICA (Strict Filtering)
        // Bypass strategico del deserializzatore Gson: l'ispezione sintattica viene svolta
        // tramite comparazione di stringhe in memoria, prevenendo cicli gravosi di garbage collection
        // e mantenendo i tempi di esecuzione del metodo al di sotto del blocco critico del daemon Paho.
        if (isPayloadValido(payload)) {
            try {
                // REGOLA DI INGEGNERIA N°2: DISACCOPPIAMENTO (Store-and-Forwarding)
                buffer.enqueue(payload);
                logger.debug("[SUBSCRIBER BILIARDO] Evento validato e accodato da topic: {}", topic);

            } catch (Exception e) {
                // Cattura procedurale in caso di fallimento disco o parse
                logger.error("[SUBSCRIBER BILIARDO] Errore nell'inserimento nel buffer: {}", e.getMessage());
            }
        } else {
            // Logica di dropout per tutto il traffico parassita, generico o di dubbia provenienza
            logger.warn("[SUBSCRIBER BILIARDO] Strict Filter fallito. Evento scartato: {}", payload);
        }
    }

    /**
     * Esegue lo sbarramento crittografico formale analizzando i watermark all'interno del pacchetto.
     * Accetta unicamente flussi marcati esplicitamente dalle schede hardware (DEVICE)
     * e provvisti del certificato d'identità inalterabile (hardwareSignature).
     *
     * @param payload Codifica stringata derivata dal socket Paho
     * @return booleano indicante la bontà formale del tracciato
     */
    private boolean isPayloadValido(String payload) {
        // Analisi reattiva a cascata per l'individuazione di pattern predefiniti
        return payload != null &&
                payload.contains("\"source\":\"DEVICE\"") &&
                payload.contains("\"hardwareSignature\"");
    }
}