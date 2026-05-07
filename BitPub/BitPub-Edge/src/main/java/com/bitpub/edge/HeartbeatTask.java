package com.bitpub.edge;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Task operativo progettato per la trasmissione ciclica della telemetria di base (Heartbeat)
 * verso l'infrastruttura Cloud. Implementando l'interfaccia Runnable, l'oggetto è concepito
 * per essere iniettato in un pool di thread temporizzato (ScheduledExecutorService),
 * isolando completamente la responsabilità del keep-alive dal thread principale di intercettazione eventi.
 * Questo disaccoppiamento architetturale garantisce che eventuali latenze o micro-cadute
 * di rete non congelino le logiche di gioco dei simulatori fisici.
 *
 * @author Stefano Bellan 20054330
 */
public class HeartbeatTask implements Runnable {

    // Meccanismo di tracciamento diagnostico ancorato al motore SLF4J
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatTask.class);

    // Riferimento immutabile al bridge di comunicazione verso il broker remoto
    private final IMqttClient mqttClient;

    // Stringa pre-calcolata contenente il percorso esatto di pubblicazione per ridurre
    // le concatenazioni superflue durante le iterazioni del ciclo
    private final String statusTopic;

    /**
     * Costruttore principale incaricato della configurazione del task di monitoraggio.
     * Fissa in modo immutabile l'indirizzo logico del topic concatenando l'identificativo
     * univoco della sede, garantendo thread-safety nativa grazie alla keyword final.
     *
     * @param mqttClient Astrazione Paho per l'accesso diretto ai metodi di pubblicazione
     * @param venueId Parametro distintivo del locale fisico da iniettare nel topic di destinazione
     */
    public HeartbeatTask(IMqttClient mqttClient, String venueId) {
        this.mqttClient = mqttClient;
        this.statusTopic = "bitpub/locali/" + venueId + "/status";
    }

    /**
     * Routine di esecuzione invocata periodicamente dall'orchestratore di sistema.
     * Esegue una verifica conservativa dello stato del socket prima di allocare messaggi,
     * compone un payload minimale conforme alle specifiche del backend e gestisce in modo
     * difensivo le eccezioni di trasporto per impedire la morte prematura dello scheduler.
     */
    @Override
    public void run() {
        try {
            // Evita di saturare i log di sistema e i buffer interni di Paho invocando la pubblicazione su un socket già chiuso
            if (mqttClient.isConnected()) {

                // Formattazione essenziale del payload JSON in linea per minimizzare l'overhead del Garbage Collector,
                // evitando l'allocazione di un intero albero Gson per un singolo campo statico
                String payload = "{\"status\":\"ONLINE\"}";
                MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));

                // Innalza il livello di Quality of Service a 1 (At Least Once) per garantire l'effettivo recapito
                // del ping vitale all'infrastruttura Cloud, compensando eventuali perdite di pacchetti sulla rete LAN
                message.setQos(1);

                try {
                    // Propagazione del messaggio serializzato attraverso il tunnel crittografato
                    mqttClient.publish(statusTopic, message);
                    logger.debug("[HEARTBEAT] Segnale ONLINE inviato correttamente su {}", statusTopic);
                } catch (MqttException me) {
                    // Intercettazione silente dell'anomalia di trasporto: impedisce che l'eccezione
                    // si propaghi al thread pool dell'executor, il quale altrimenti dismetterebbe permanentemente la schedulazione del task
                    logger.warn("[HEARTBEAT] Cloud non raggiungibile (MqttException). Riprovo al prossimo ciclo. Errore: {}", me.getMessage());
                }

            } else {
                logger.debug("[HEARTBEAT] Invio saltato: client MQTT attualmente disconnesso.");
            }
        } catch (Exception e) {
            // Scudo architetturale estremo per blindare il Runnable contro qualsiasi RuntimeException
            // non prevista (es. OutOfMemoryError transitorio, NullPointerException), preservando il ciclo di vita dell'Edge
            logger.error("[HEARTBEAT] Errore critico imprevisto nel task: {}", e.getMessage());
        }
    }
}