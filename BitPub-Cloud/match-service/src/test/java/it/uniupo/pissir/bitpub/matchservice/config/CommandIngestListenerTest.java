// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.matchservice.dto.GameActionRequestDto;
import it.uniupo.pissir.bitpub.matchservice.service.impl.MatchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandIngestListenerTest {

    @Mock private MatchServiceImpl matchService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private CommandIngestListener listener() {
        return new CommandIngestListener(matchService, objectMapper);
    }

    private String wrap(String actorUserId, String targetId, String innerPayload) throws Exception {
        return objectMapper.writeValueAsString(new MqttCommandWrapper(actorUserId, "PLAYER", targetId, innerPayload));
    }

    @Test
    void onGameAction_valid_forwardsToProcessGameAction() throws Exception {
        String inner = objectMapper.writeValueAsString(GameActionRequestDto.builder().sensorType("GOAL").eventId("e1").build());
        String payload = wrap("pA", "m1", inner);

        listener().onGameAction(new GenericMessage<>(payload));

        ArgumentCaptor<GameActionRequestDto> action = ArgumentCaptor.forClass(GameActionRequestDto.class);
        verify(matchService).processGameAction(eq("m1"), eq("pA"), action.capture());
        assertThat(action.getValue().getSensorType()).isEqualTo("GOAL");
    }

    @Test
    void onGameAction_serviceRejects_swallowsBitpubException() throws Exception {
        String inner = objectMapper.writeValueAsString(GameActionRequestDto.builder().sensorType("GOAL").build());
        String payload = wrap("pA", "m1", inner);
        when(matchService.processGameAction(any(), any(), any()))
                .thenThrow(new BitpubException("not your turn", HttpStatus.FORBIDDEN));

        // Un rifiuto legittimo dell'azione non deve propagare eccezioni
        // che innescherebbero un re-inoltro improprio del messaggio (QoS 1) da parte del broker.
        assertThatCode(() -> listener().onGameAction(new GenericMessage<>(payload))).doesNotThrowAnyException();
    }

    @Test
    void onGameAction_malformedPayload_swallowsAndDoesNotCallService() {
        assertThatCode(() -> listener().onGameAction(new GenericMessage<>("not-json"))).doesNotThrowAnyException();
        verify(matchService, never()).processGameAction(any(), any(), any());
    }
}
