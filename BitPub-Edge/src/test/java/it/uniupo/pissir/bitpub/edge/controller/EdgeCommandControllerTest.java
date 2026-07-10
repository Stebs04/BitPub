/**
 * Autore: Timothy Giolito 20054431
 */
package it.uniupo.pissir.bitpub.edge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import it.uniupo.pissir.bitpub.edge.service.MqttBufferService;
import it.uniupo.pissir.bitpub.edge.service.RuleEngineService;
import it.uniupo.pissir.bitpub.edge.service.RuleEngineService.LocalMatchState;
import org.springframework.messaging.MessageChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Test unitario per il Controller. Siccome l'Edge non adotta spring-security ma valida i JWT autonomamente,
// non abbiamo bisogno di usare MockMvc; possiamo testare la logica di turnazione e instradamento isolatamente.
@ExtendWith(MockitoExtension.class)
class EdgeCommandControllerTest {

    @Mock private JwtUtils jwtUtils;
    @Mock private MqttBufferService mqttBuffer;
    @Mock private RuleEngineService ruleEngine;
    @Mock private MessageChannel localMqttOutboundChannel;

    private EdgeCommandController controller;

    @BeforeEach
    void setUp() {
        controller = new EdgeCommandController(jwtUtils, new ObjectMapper(), mqttBuffer, ruleEngine, localMqttOutboundChannel);
    }

    @Test
    void gameAction_missingBearer_unauthorized() {
        assertThatThrownBy(() -> controller.gameAction("m1", "{}", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(mqttBuffer, never()).send(any(), any());
    }

    @Test
    void gameAction_invalidToken_unauthorized() {
        when(jwtUtils.validateToken("tok")).thenReturn(false);

        assertThatThrownBy(() -> controller.gameAction("m1", "{}", "Bearer tok"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void gameAction_outOfTurn_forbidden_notPublished() {
        when(jwtUtils.validateToken("tok")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("tok")).thenReturn("u1");
        when(jwtUtils.getRoleFromToken("tok")).thenReturn("PLAYER");
        when(ruleEngine.isPlayersTurn("m1", "u1")).thenReturn(false);

        assertThatThrownBy(() -> controller.gameAction("m1", "{}", "Bearer tok"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(localMqttOutboundChannel, never()).send(any()); // Blocco preventivo, il simulatore locale non deve essere chiamato
    }

    @Test
    void gameAction_validTurn_routesToLocalSimulatorAndReturns202() {
        when(jwtUtils.validateToken("tok")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("tok")).thenReturn("u1");
        when(jwtUtils.getRoleFromToken("tok")).thenReturn("PLAYER");
        when(ruleEngine.isPlayersTurn(eq("m1"), eq("u1"))).thenReturn(true);

        LocalMatchState state = new LocalMatchState();
        state.matchId = "m1";
        state.gameInstanceId = "gi1";
        state.localeId = "loc1";
        state.gameTypeId = "foosball";
        state.teamOrder.add("Team A");
        state.playerUserIds.add("u1");
        when(ruleEngine.getState("m1")).thenReturn(state);

        var response = controller.gameAction("m1", "{\"sensorType\":\"GOAL\",\"eventId\":\"e1\"}", "Bearer tok");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(localMqttOutboundChannel).send(any()); // Inviato direttamente in locale senza passare dal buffer verso il Cloud
        verify(mqttBuffer, never()).send(any(), any());
    }

    @Test
    void tournamentResult_validToken_publishesToCloud() {
        when(jwtUtils.validateToken("tok")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("tok")).thenReturn("admin1");
        when(jwtUtils.getRoleFromToken("tok")).thenReturn("LOCALE_ADMIN");

        var response = controller.tournamentResult("t1", "tm1", "winner1", "2-1", "Bearer tok");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(mqttBuffer).send(any(), any());
    }
}
