// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.matchservice.domain.Match;
import it.uniupo.pissir.bitpub.matchservice.domain.MatchParticipant;
import it.uniupo.pissir.bitpub.matchservice.dto.GameActionRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.JoinLobbyRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import it.uniupo.pissir.bitpub.matchservice.repository.MatchParticipantRepository;
import it.uniupo.pissir.bitpub.matchservice.repository.MatchRepository;
import it.uniupo.pissir.bitpub.matchservice.repository.SensorEventLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MatchParticipantRepository participantRepository;
    @Mock private SensorEventLogRepository sensorEventLogRepository;
    @Mock private MessageChannel mqttOutboundChannel;

    private MatchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MatchServiceImpl(matchRepository, participantRepository, sensorEventLogRepository, new ObjectMapper());
        // Essendo mqttOutboundChannel qualificato con @Autowired/@Qualifier, non è gestito dal costruttore;
        // l'iniezione avviene tramite manipolazione reflection per favorire il testing isolato.
        ReflectionTestUtils.setField(service, "mqttOutboundChannel", mqttOutboundChannel);
    }

    private MatchParticipant team(String id, String name, int score, String... playerIds) {
        return MatchParticipant.builder().id(id).name(name).score(score)
                .playerIds(new ArrayList<>(List.of(playerIds))).build();
    }

    private Match match(String id, String status, MatchParticipant... teams) {
        return Match.builder().id(id).gameInstanceId("gi1").localeId("loc1").gameTypeId("pool")
                .status(status).startTime(Instant.now())
                .teams(new ArrayList<>(List.of(teams))).build();
    }

    // ── Verifica Transizioni Matchmaking: da WAITING_FOR_PLAYERS a IN_PROGRESS ──
    @Test
    void joinLobby_secondPlayer_transitionsToInProgress() {
        Match waiting = match("m1", "WAITING_FOR_PLAYERS", team("tA", "alice", 0, "pA"));
        when(matchRepository.findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc("gi1", "WAITING_FOR_PLAYERS"))
                .thenReturn(Optional.of(waiting));
        when(participantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MatchDto dto = service.joinLobby(new JoinLobbyRequestDto("gi1", "bob", null), "pB");

        assertThat(dto.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(dto.getTeams()).hasSize(2);
        assertThat(dto.getCurrentTurnUserId()).isEqualTo("pA"); // Il turno inaugurale viene concesso al creatore della lobby
        verify(mqttOutboundChannel, atLeastOnce()).send(any()); // Certifica la pubblicazione dello stato live sul broker
    }

    @Test
    void joinLobby_samePlayerReconnect_isIdempotent() {
        Match waiting = match("m1", "WAITING_FOR_PLAYERS", team("tA", "alice", 0, "pA"));
        when(matchRepository.findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc("gi1", "WAITING_FOR_PLAYERS"))
                .thenReturn(Optional.of(waiting));

        MatchDto dto = service.joinLobby(new JoinLobbyRequestDto("gi1", "alice", null), "pA");

        assertThat(dto.getStatus()).isEqualTo("WAITING_FOR_PLAYERS");
        assertThat(dto.getTeams()).hasSize(1);
        verify(participantRepository, never()).save(any());
    }

    // ── Validazione Funzione endMatch ──
    @Test
    void endMatch_completesAndComputesWinner() {
        Match m = match("m1", "IN_PROGRESS", team("tA", "alice", 10, "pA"), team("tB", "bob", 5, "pB"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(m));
        when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MatchDto dto = service.endMatch("m1");

        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        assertThat(dto.getWinnerId()).isEqualTo("pA"); // Accertamento del punteggio maggiore
        verify(mqttOutboundChannel, atLeastOnce()).send(any());
    }

    @Test
    void endMatch_alreadyCompleted_isIdempotent() {
        Match m = match("m1", "COMPLETED", team("tA", "alice", 10, "pA"), team("tB", "bob", 5, "pB"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(m));

        service.endMatch("m1");

        verify(matchRepository, never()).save(any());
    }

    // ── Validazione Funzione applyFinalResult ──
    @Test
    void applyFinalResult_appliesScoresByTeamName_andCompletes() {
        Match m = match("m1", "IN_PROGRESS", team("tA", "RED", 0, "pA"), team("tB", "BLUE", 0, "pB"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(m));
        when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MatchDto dto = service.applyFinalResult("m1", Map.of("RED", 3, "BLUE", 1));

        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        assertThat(dto.getWinnerId()).isEqualTo("pA");
        assertThat(dto.getTeams()).filteredOn(t -> t.getName().equals("RED")).first()
                .extracting(t -> t.getScore()).isEqualTo(3);
    }

    @Test
    void applyFinalResult_tie_hasNoWinner() {
        Match m = match("m1", "IN_PROGRESS", team("tA", "RED", 0, "pA"), team("tB", "BLUE", 0, "pB"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(m));
        when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MatchDto dto = service.applyFinalResult("m1", Map.of("RED", 2, "BLUE", 2));

        assertThat(dto.getWinnerId()).isNull(); // In caso di pareggio si omette il vincitore e la classifica non viene alterata
        verify(mqttOutboundChannel, never()).send(any());
    }

    // ── Validazione Funzione processGameAction ──
    @Test
    void processGameAction_matchNotInProgress_conflict() {
        Match m = match("m1", "COMPLETED", team("tA", "alice", 0, "pA"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.processGameAction("m1", "pA", GameActionRequestDto.builder().sensorType("GOAL").build()))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void processGameAction_missingSensorType_badRequest() {
        Match m = match("m1", "IN_PROGRESS", team("tA", "alice", 0, "pA"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.processGameAction("m1", "pA", GameActionRequestDto.builder().sensorType(" ").build()))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void processGameAction_valid_routesToSimulatorViaMqtt() {
        Match m = match("m1", "IN_PROGRESS", team("tA", "alice", 0, "pA"));
        when(matchRepository.findById("m1")).thenReturn(Optional.of(m));

        service.processGameAction("m1", "pA", GameActionRequestDto.builder().sensorType("GOAL").eventId("e1").build());

        verify(mqttOutboundChannel).send(any()); // Riscontro dell'avvenuto inoltro al simulatore tramite MQTT
    }

    // ── Validazione Funzione processSensorEvent ──
    @Test
    void processSensorEvent_duplicateEventId_skips() {
        SensorEvent event = SensorEvent.builder().eventId(UUID.randomUUID()).gameInstanceId("gi1").sensorType("GOAL").build();
        when(sensorEventLogRepository.existsByEventId(event.getEventId().toString())).thenReturn(true);

        service.processSensorEvent(event);

        verify(matchRepository, never()).save(any());
        verify(sensorEventLogRepository, never()).save(any());
    }

    @Test
    void processSensorEvent_matchEnd_closesActiveMatch() {
        Match m = match("m1", "IN_PROGRESS", team("tA", "alice", 3, "pA"), team("tB", "bob", 1, "pB"));
        SensorEvent event = SensorEvent.builder().eventId(UUID.randomUUID()).gameInstanceId("gi1").sensorType("MATCH_END").build();
        when(sensorEventLogRepository.existsByEventId(any())).thenReturn(false);
        when(matchRepository.findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc("gi1", "IN_PROGRESS"))
                .thenReturn(Optional.of(m));

        service.processSensorEvent(event);

        assertThat(m.getStatus()).isEqualTo("COMPLETED");
        verify(sensorEventLogRepository).save(any()); // L'evento simulato deve essere tracciato integralmente
    }

    // ── Validazione Controlli su startMatch ──
    @Test
    void startMatch_existingInProgress_throwsIllegalState() {
        when(matchRepository.findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc("gi1", "IN_PROGRESS"))
                .thenReturn(Optional.of(match("existing", "IN_PROGRESS", team("tA", "x", 0, "pX"))));

        StartMatchRequestDto req = new StartMatchRequestDto();
        req.setGameInstanceId("gi1");

        assertThatThrownBy(() -> service.startMatch(req)).isInstanceOf(IllegalStateException.class);
    }
}
