package com.bitpub.edge;

import com.bitpub.buffer.PersistentEventStore;
import com.bitpub.mqtt.CloudMqttManager;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.SSLContext;

/**
 * Tunnel di comunicazione asincrono per il nodo periferico Edge.
 * L'architettura è stata ingegnerizzata per operare come gateway bidirezionale:
 * intercetta e disaccoppia i comandi direttivi imposti dall'infrastruttura Cloud,
 * smistandoli localmente ai moduli di simulazione fisica, e contestualmente offre
 * il varco di accesso per incanalare la telemetria di gioco all'interno
 * del buffer Store-and-Forward di salvaguardia.
 *
 */
public class EdgeMqttClient implements MqttCallback {

    // Meccanismo di logging granulare demandato al framework SLF4J
    private static final Logger logger = LoggerFactory.getLogger(EdgeMqttClient.class);

    // Gestore del socket persistente implementato dalla libreria Eclipse Paho
    private MqttClient client;

    // Modulo incaricato di supervisionare lo statemap a stati finiti dei tavoli locali
    private final GameTableStateManager stateManager;

    // Memoria circolare bloccante necessaria per tutelare l'integrità del dato fisico
    private final PersistentEventStore buffer;

    // Contesto crittografico per l'autenticazione mTLS verso il broker cloud
    private final SSLContext sslContext;
    private final com.bitpub.sync.MqttSessionManager sessionManager;

    /**
     * Costruttore parametrico. Alloca l'istanza vincolandola in maniera immutabile
     * alle due componenti strategiche del nodo: lo stato operazionale e la memoria transitoria.
     *
     * @param stateManager Struttura dati per la validazione logica dei tavoli in uso
     * @param buffer Astrazione a coda in cui rovesciare la telemetria ottica
     * @param sslContext Contesto crittografico pre-valutato per il tunnel mTLS
     */
    public EdgeMqttClient(GameTableStateManager stateManager, PersistentEventStore buffer, SSLContext sslContext, com.bitpub.sync.MqttSessionManager sessionManager) {
        this.stateManager = stateManager;
        this.buffer = buffer;
        this.sslContext = sslContext;
        this.sessionManager = sessionManager;
    }

    /**
     * Orchestra il protocollo di allineamento crittografato verso il broker centrale.
     * Sfrutta un manager esterno per mimetizzare le procedure di handshaking TLS
     * e si sottoscrive automaticamente ai canali di comando riservati al proprio profilo di autorizzazione.
     */
    public void connect() {
        try {
            // Risoluzione della catena di trust SSL e allocazione dei certificati client
            // incapsulati per mantenere compatto il corpo logico del controller
            this.client = CloudMqttManager.configuraClientCloud("localhost", "Locale_1", sslContext);

            // Registrazione dell'istanza corrente come ascoltatore reattivo per gli interrupt di rete
            this.client.setCallback(this);

            // Vincolo su Quality of Service 1 (At Least Once) per garantire l'affidabilità
            // della direttiva di blocco/sblocco del macchinario
            client.subscribe("bitpub/cloud/foosball/+", 1);
            client.subscribe("bitpub/cloud/admin/+", 1);
            
            if (sessionManager != null) {
                sessionManager.connectComplete(false, client.getServerURI());
            }

            logger.info("[EDGE MQTT] Connesso e sottoscritto ai canali di comando.");

        } catch (Exception e) {
            // Fail-safe intercettato dal logger senza causare lo spegnimento forzato
            // poiché il sistema bufferizzato potrebbe ancora raccogliere e accantonare dati locali
            logger.error("[EDGE MQTT] Errore critico durante la connessione", e);
        }
    }

    /**
     * Innesca la disconnessione graziosa del socket MQTT.
     * Passaggio tassativo per scongiurare che il broker cloud identifichi lo spegnimento
     * come anomalo, scatenando di conseguenza falsi positivi nei monitoraggi di disponibilità (LWT).
     */
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                logger.info("[EDGE MQTT] Disconnessione effettuata con successo.");
            }
        } catch (MqttException e) {
            logger.error("[EDGE MQTT] Errore durante la disconnessione: {}", e.getMessage());
        }
    }

    /**
     * Espone in sola lettura il client nativo ai processi di background
     * che necessitano di un canale diretto (es. invio Heartbeat).
     *
     * @return L'interfaccia Paho operativa
     */
    public IMqttClient getClient() {
        return client;
    }

    /**
     * Ritorna l'handler della coda per permettere ai moduli listener
     * di enqueuare le matrici di sensori raccolte sul campo.
     *
     * @return L'istanza del sistema Store-and-Forward
     */
    public PersistentEventStore getBuffer() {
        return buffer;
    }

    // --- Implementazione MqttCallback ---

    /**
     * Hook generato nativamente dal daemon Paho a fronte dell'ingresso di un pacchetto.
     * Applica il pattern di routing asincrono: distacca immediatamente il payload
     * su un worker thread per non occupare il dispatcher di rete.
     *
     * @param topic Il percorso logico esatto che ha consegnato la direttiva
     * @param message L'involucro contenente il payload informativo e i flag QoS
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        logger.info("[EDGE MQTT] Messaggio ricevuto sul topic: {}", topic);

        // Disaccoppiamento architetturale: l'elaborazione del comando
        // e la conseguente manipolazione degli statemap vengono processate fuori dal contesto MQTT
        new Thread(() -> {
            try {
                new AdminCommandListener(stateManager).messageArrived(topic, message);
            } catch (Exception e) {
                logger.error("[EDGE MQTT] Errore nel dispatch del comando amministrativo: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * Attiva le logiche di fallback se il bridge verso il cloud cade inavvertitamente.
     */
    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("[EDGE MQTT] Connessione persa: {}", cause != null ? cause.getMessage() : "Sconosciuta");
        if (sessionManager != null) {
            sessionManager.connectionLost(cause);
        }
    }

    /**
     * Feedback opzionale ignorato per design: i comandi emessi localmente dall'Edge
     * vengono già gestiti dall'architettura Store-and-Forward.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Nessun comportamento codificato
    }
}