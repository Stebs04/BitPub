package it.uniupo.pissir.bitpub.edge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Buffer offline esplicito a livello applicativo per l'egress Edge -> Cloud su MQTT. NON si affida
 * alla coda invisibile di Paho/broker: traccia lo stato della connessione MQTT verso il Cloud
 * (UP/DOWN) tramite gli eventi di Spring Integration e, quando e' DOWN, accoda in memoria i risultati
 * delle partite e i comandi di torneo, per poi svuotare la coda alla riconnessione con log espliciti.
 * Garantisce che l'esito di una partita/torneo attivo non vada perso durante un'interruzione del Cloud.
 *
 * ponytail: coda in memoria; persa a un riavvio del processo Edge. Persistere sull'H2 edge-db solo se
 * serve la sopravvivenza anche a un crash dell'Edge (il requisito riguarda la caduta del Cloud).
 */
@Service
@Slf4j
public class MqttBufferService {

    private final MessageChannel cloudMqttOutboundChannel;
    private final Queue<Message<?>> buffer = new ConcurrentLinkedQueue<>();
    private volatile boolean cloudUp = true;

    public MqttBufferService(@Qualifier("cloudMqttOutboundChannel") MessageChannel cloudMqttOutboundChannel) {
        this.cloudMqttOutboundChannel = cloudMqttOutboundChannel;
    }

    /**
     * Invia il messaggio al Cloud se la connessione e' UP, altrimenti lo accoda localmente.
     * {@code description} identifica l'azione bufferizzata nel log offline.
     */
    public void send(Message<?> message, String description) {
        if (cloudUp) {
            try {
                cloudMqttOutboundChannel.send(message);
                return;
            } catch (Exception e) {
                // Broker/Cloud caduto tra un evento e l'altro: ripiega sul buffer e marca DOWN.
                cloudUp = false;
                log.warn("Cloud send failed, switching to offline buffer", e);
            }
        }
        buffer.add(message);
        log.warn("[OFFLINE] Cloud unreachable: queuing {} (buffer size {})", description, buffer.size());
    }

    @EventListener
    public void onConnectionFailed(MqttConnectionFailedEvent event) {
        if (cloudUp) {
            log.warn("[OFFLINE] Cloud MQTT connection lost; buffering egress locally", event.getCause());
        }
        cloudUp = false;
    }

    @EventListener
    public void onSubscribed(MqttSubscribedEvent event) {
        // (Ri)connessione + subscribe riuscita: connessione UP, svuota la coda accumulata.
        cloudUp = true;
        flush();
    }

    private synchronized void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        int flushed = 0;
        Message<?> m;
        while ((m = buffer.peek()) != null) {
            try {
                cloudMqttOutboundChannel.send(m);
                buffer.poll();
                flushed++;
            } catch (Exception e) {
                // Ancora irraggiungibile: lascia il resto in coda per il prossimo tentativo.
                cloudUp = false;
                log.error("Flush interrupted; {} messages still queued", buffer.size(), e);
                break;
            }
        }
        if (flushed > 0) {
            log.info("[ONLINE] Cloud connection restored: flushed {} queued messages from buffer.", flushed);
        }
    }
}
