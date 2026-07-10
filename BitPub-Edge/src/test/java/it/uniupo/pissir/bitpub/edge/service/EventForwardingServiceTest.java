/**
 * Autore: Timothy Giolito 20054431
 */
package it.uniupo.pissir.bitpub.edge.service;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
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
    @Mock private MqttBufferService mqttBuffer;

    private EventForwardingService service;

    @BeforeEach
    void setUp() {
        service = new EventForwardingService(ruleEngineService, mqttBuffer);
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

        verify(mqttBuffer, never()).send(any(), any());
    }

    @Test
    void handleMqttMessage_validEventNoLiveState_forwardsToCloudOnce() {
        SensorEvent e = event();
        when(ruleEngineService.validateAndParse(any())).thenReturn(Optional.of(e));
        when(ruleEngineService.applyEvent(e)).thenReturn(Optional.empty());

        service.handleMqttMessage(msg("{...}"));

        verify(mqttBuffer, times(1)).send(any(), any()); // Controlliamo che avvenga unicamente l'inoltro verso il Cloud
        verify(ruleEngineService, never()).clearState(any());
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
        when(ruleEngineService.buildResultPayload(state)).thenReturn(Map.of("matchId", "m1"));

        service.handleMqttMessage(msg("{...}"));

        // In totale ci aspettiamo tre messaggi inviati: uno per lo stato locale, uno per il resoconto e l'ultimo per l'inoltro verso il cloud
        verify(mqttBuffer, times(3)).send(any(), any());
        verify(ruleEngineService).clearState("m1"); // Una volta finita la partita, ci assicuriamo che lo stato in memoria venga rimosso
    }

    @Test
    void handleMatchSync_initializesLocalState() {
        String syncJson = "{\"id\":\"m1\",\"status\":\"IN_PROGRESS\"}";

        service.handleMatchSync(msg(syncJson));

        verify(ruleEngineService).initFromSync(argThatHasId());
    }

    private static com.fasterxml.jackson.databind.JsonNode argThatHasId() {
        return org.mockito.ArgumentMatchers.argThat(n -> n != null && "m1".equals(n.path("id").asText()));
    }

    @Test
    void handleMatchSync_invalidJson_swallowed() {
        service.handleMatchSync(msg("{ not json"));

        verify(ruleEngineService, never()).initFromSync(any());
    }
}
