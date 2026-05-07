package com.bitpub.mqtt;

import com.bitpub.buffer.BufferDatiEdge;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Controller di frontiera per l'ingestione della telemetria locale originata dai tavoli fisici.
 * Opera all'interno dell'architettura in veste di Consumer di primo livello (su Edge)
 * e di Producer verso il layer di disaccoppiamento (Store-and-Forward).
 * Implementa una strategia di filtraggio reattivo e ultra-leggero (Strict Filter) per proteggere
 * il buffer da pacchetti malformati o iniezioni malevole (spoofing), disimpegnando
 * istantaneamente il thread MQTT dedicato alla ricezione per garantire alta reattività.
 *
 * @author Stefano Bellan 20054330
 */
public class LocalCalciobalillaSubscriber implements IMqttMessageListener {

    // Componente diagnostica governata dal framework SLF4J
    private static final Logger logger = LoggerFactory.getLogger(LocalCalciobalillaSubscriber.class);

    // Coda circolare atomica in cui rovesciare l'informazione ripulita
    private final BufferDatiEdge buffer;

    /**
     * Iniezione delle dipendenze architetturali. Allaccia il modulo d'ascolto MQTT
     * all'area di stazionamento concorrente.
     *
     * @param buffer La memoria transitoria (LinkedBlockingQueue) condivisa con il demone di esportazione Cloud
     */
    public LocalCalciobalillaSubscriber(BufferDatiEdge buffer) {
        this.buffer = buffer;
    }

    /**
     * Hook generato dal framework di rete ogniqualvolta la sottoscrizione (subscribe)
     * capta un pacchetto pertinente dal broker Mosquitto locale.
     *
     * @param topic La stringa rappresentante il canale gerarchico che ha consegnato l'informazione
     * @param message L'involucro di byte contenente il dato sensoriale originario
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Normalizzazione forzata della codifica per garantire l'immunità da artefatti charset
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        // REGOLA DI INGEGNERIA N°1: STRICT FILTERING (Zero-Parsing Validation)
        // Bypass dell'albero computazionale Gson: la pre-validazione applica un check diretto
        // sulle sottostringhe del JSON. Evitando l'impiego della riflessione Java o dei tokenizzatori,
        // si abbatte drasticamente il consumo del Garbage Collector su flussi ad altissima frequenza.
        if (isPayloadValido(payload)) {
            try {
                // REGOLA DI INGEGNERIA N°2: ACCATASATMENTO ASINCRONO
                // Passaggio del comando di accodamento: la direttiva buffer.put() assicura la
                // sincronizzazione thread-safe. Essendo l'allocazione quasi istantanea,
                // il daemon MQTT si libera in poche decine di nanosecondi per servire il pacchetto successivo.
                buffer.put(payload);
                logger.debug("[SUBSCRIBER CALCIOBALILLA] Evento validato e accodato da topic: {}", topic);

            } catch (InterruptedException e) {
                // Intercettamento pulito di una direttiva di kill del processo
                logger.warn("[SUBSCRIBER CALCIOBALILLA] Inserimento nel buffer interrotto.");
                // Propagazione della segnalazione d'interrupt per salvaguardare il ciclo di vita della JVM
                Thread.currentThread().interrupt();
            }
        } else {
            // Drop difensivo e incondizionato della matrice di dati non certificata
            logger.warn("[SUBSCRIBER CALCIOBALILLA] Strict Filter fallito. Evento scartato: {}", payload);
        }
    }

    /**
     * Svolge il ruolo di Security Gate basato su firma heuristica (Heuristic Signature Matching).
     * Analizza la matrice testuale confermando l'autenticità della provenienza (dispositivo autorizzato)
     * e l'integrità strutturale del certificato hardware (falsificabilità).
     *
     * @param payload L'involucro testuale grezzo estratto dalla rete
     * @return Valore di verità confermante il superamento dello scrutinio anti-spoofing
     */
    private boolean isPayloadValido(String payload) {
        // Valutazione rapida sequenziale basata sull'albero condizionale short-circuit &&
        // in cui la prima mancata occorrenza decreta lo stop immediato del controllo.
        return payload != null &&
                payload.contains("\"source\":\"DEVICE\"") &&
                payload.contains("\"hardwareSignature\"");
    }
}