/**
 * Autore: Timothy Giolito 20054431
 */
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
 * Servizio di buffering offline a livello applicativo per le comunicazioni in uscita dall'Edge verso il Cloud.
 * Evitiamo di affidarci unicamente alle code interne del broker e tracciamo esplicitamente lo stato della connessione.
 * In caso di disconnessione, i risultati e i comandi vengono accodati in memoria, per poi essere inviati
 * non appena la connessione torna attiva, garantendo l'integrità dei dati.
 * Attualmente la coda vive solo in memoria ed è pensata per gestire le cadute del Cloud, non i crash locali.
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
     * Tenta l'invio del messaggio verso il Cloud.
     * Se la connessione risulta inattiva, salva il messaggio localmente nella coda.
     */
    public void send(Message<?> message, String description) {
        if (cloudUp) {
            try {
                cloudMqttOutboundChannel.send(message);
                return;
            } catch (Exception e) {
                // Il sistema remoto è caduto tra un controllo e l'altro, passiamo al buffer e segniamo offline.
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
        // Sottoscrizione avvenuta con successo, il Cloud è di nuovo raggiungibile, svuotiamo la coda.
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
                // Il server è di nuovo irraggiungibile, lasciamo i restanti messaggi in coda per la prossima volta.
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
