/**
 * Autore: Timothy Giolito 20054431
 */
package it.uniupo.pissir.bitpub.edge.service;

import org.junit.jupiter.api.Test;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MqttBufferServiceTest {

    private Message<String> msg(String s) {
        return new GenericMessage<>(s);
    }

    @Test
    void down_queuesInsteadOfSending_thenFlushesOnReconnect() {
        MessageChannel channel = mock(MessageChannel.class);
        MqttBufferService buffer = new MqttBufferService(channel);

        // Simulo la caduta del Cloud: gli invii successivi devono essere messi in coda, non inoltrati.
        buffer.onConnectionFailed(new MqttConnectionFailedEvent(this, new RuntimeException("lost")));
        buffer.send(msg("r1"), "result 1");
        buffer.send(msg("r2"), "result 2");
        verify(channel, never()).send(any());

        // Alla riconnessione verifichiamo che tutto il buffer venga svuotato nell'ordine corretto.
        buffer.onSubscribed(new MqttSubscribedEvent(this, "topic"));
        verify(channel, times(2)).send(any());
    }

    @Test
    void up_sendsThrough_noQueue() {
        MessageChannel channel = mock(MessageChannel.class);
        MqttBufferService buffer = new MqttBufferService(channel); // Per default consideriamo la connessione attiva

        buffer.send(msg("r1"), "result 1");

        verify(channel, times(1)).send(any());
        // Un eventuale evento di riconnessione non troverà niente in coda da svuotare.
        buffer.onSubscribed(new MqttSubscribedEvent(this, "topic"));
        verify(channel, times(1)).send(any());
    }

    @Test
    void sendFailure_fallsBackToBuffer_thenFlushesWhenChannelRecovers() {
        MessageChannel channel = mock(MessageChannel.class);
        // Simulo che il broker cada proprio durante l'invio: il messaggio andrà in coda anche se il sistema lo riteneva attivo.
        org.mockito.Mockito.doThrow(new RuntimeException("broker down")).doReturn(true).when(channel).send(any());
        MqttBufferService buffer = new MqttBufferService(channel);

        buffer.send(msg("r1"), "result 1"); // L'eccezione interna viene catturata e il messaggio va in coda
        verify(channel, times(1)).send(any());

        // Al ritorno della connessione, il canale funziona di nuovo e il messaggio in coda viene smaltito con successo.
        buffer.onSubscribed(new MqttSubscribedEvent(this, "topic"));
        verify(channel, times(2)).send(any());
    }
}
