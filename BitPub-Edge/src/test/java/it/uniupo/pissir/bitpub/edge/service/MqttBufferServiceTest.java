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

        // Cloud drops: subsequent sends are queued, not forwarded.
        buffer.onConnectionFailed(new MqttConnectionFailedEvent(this, new RuntimeException("lost")));
        buffer.send(msg("r1"), "result 1");
        buffer.send(msg("r2"), "result 2");
        verify(channel, never()).send(any());

        // Reconnect: the whole buffer is flushed in order.
        buffer.onSubscribed(new MqttSubscribedEvent(this, "topic"));
        verify(channel, times(2)).send(any());
    }

    @Test
    void up_sendsThrough_noQueue() {
        MessageChannel channel = mock(MessageChannel.class);
        MqttBufferService buffer = new MqttBufferService(channel); // cloudUp=true by default

        buffer.send(msg("r1"), "result 1");

        verify(channel, times(1)).send(any());
        // A later reconnect event has nothing to flush.
        buffer.onSubscribed(new MqttSubscribedEvent(this, "topic"));
        verify(channel, times(1)).send(any());
    }

    @Test
    void sendFailure_fallsBackToBuffer_thenFlushesWhenChannelRecovers() {
        MessageChannel channel = mock(MessageChannel.class);
        // First send throws (broker down mid-send) so the message is queued despite cloudUp=true.
        org.mockito.Mockito.doThrow(new RuntimeException("broker down")).doReturn(true).when(channel).send(any());
        MqttBufferService buffer = new MqttBufferService(channel);

        buffer.send(msg("r1"), "result 1"); // throws internally, gets queued
        verify(channel, times(1)).send(any());

        // Reconnect: channel now succeeds, the queued message is flushed.
        buffer.onSubscribed(new MqttSubscribedEvent(this, "topic"));
        verify(channel, times(2)).send(any());
    }
}
