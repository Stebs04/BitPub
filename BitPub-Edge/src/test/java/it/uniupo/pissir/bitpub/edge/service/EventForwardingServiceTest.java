package it.uniupo.pissir.bitpub.edge.service;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventForwardingServiceTest {

    @Mock private RuleEngineService ruleEngineService;
    @Mock private MessageChannel cloudMqttOutboundChannel;

    private EventForwardingService service;

    @BeforeEach
    void setUp() {
        service = new EventForwardingService(ruleEngineService, cloudMqttOutboundChannel);
    }

    private Message<String> msg(String payload) {
        return new GenericMessage<>(payload);
    }

    private SensorEvent event() {
        return SensorEvent.builder().eventId(UUID.randomUUID()).gameInstanceId("gi1").sensorType("GOAL").build();
    }

    @Test
    void handleMqttMessage_invalidEvent_notForwarded() {
        when(ruleEngineService.validateAndParse("bad")).thenReturn(Optional.empty());

        service.handleMqttMessage(msg("bad"));

        verify(cloudMqttOutboundChannel, never()).send(any());
    }

    @Test
    void handleMqttMessage_validEventNoLiveState_forwardsToCloudOnce() {
        SensorEvent e = event();
        when(ruleEngineService.validateAndParse(any())).thenReturn(Optional.of(e));
        when(ruleEngineService.applyEvent(e)).thenReturn(Optional.empty());

        service.handleMqttMessage(msg("{...}"));

        verify(cloudMqttOutboundChannel, times(1)).send(any()); // solo l'ingest verso il Cloud
        verify(ruleEngineService, never()).reportResultToCloud(any());
    }

    @Test
    void handleMqttMessage_finishedState_publishesStateAndReportsResult() {
        SensorEvent e = event();
        RuleEngineService.LocalMatchState state = new RuleEngineService.LocalMatchState();
        state.matchId = "m1";
        state.gameInstanceId = "gi1";
        state.localeId = "loc1";
        state.finished = true;
        when(ruleEngineService.validateAndParse(any())).thenReturn(Optional.of(e));
        when(ruleEngineService.applyEvent(e)).thenReturn(Optional.of(state));
        when(ruleEngineService.buildStatePayload(state, "GOAL")).thenReturn(Map.of("status", "FINISHED"));

        service.handleMqttMessage(msg("{...}"));

        // Un send per lo stato live locale + un send per l'ingest cloud.
        verify(cloudMqttOutboundChannel, times(2)).send(any());
        verify(ruleEngineService).reportResultToCloud(state); // fine partita: risultato al Cloud
    }
}
