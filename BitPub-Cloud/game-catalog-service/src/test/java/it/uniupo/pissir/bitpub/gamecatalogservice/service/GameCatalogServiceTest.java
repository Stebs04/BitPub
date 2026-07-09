package it.uniupo.pissir.bitpub.gamecatalogservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.gamecatalogservice.config.MqttConfig.ConfigPublisher;
import it.uniupo.pissir.bitpub.gamecatalogservice.domain.GameType;
import it.uniupo.pissir.bitpub.gamecatalogservice.domain.SensorDefinition;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.AddSensorRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.CreateGameTypeRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.GameTypeRepository;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.SensorDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameCatalogServiceTest {

    @Mock private GameTypeRepository gameTypeRepository;
    @Mock private SensorDefinitionRepository sensorDefinitionRepository;
    @Mock private ConfigPublisher configPublisher;

    private GameCatalogService service;

    @BeforeEach
    void setUp() {
        service = new GameCatalogService(gameTypeRepository, sensorDefinitionRepository, configPublisher, new ObjectMapper());
    }

    private GameType gameType(String id, String name) {
        return GameType.builder().id(id).name(name).description("d").rulesEngineId("rules_x")
                .winScoreTarget(10).sensors(new ArrayList<>()).build();
    }

    // --- createGameType ---
    @Test
    void createGameType_duplicateName_conflict() {
        when(gameTypeRepository.findByName("Pool")).thenReturn(Optional.of(gameType("g1", "Pool")));
        CreateGameTypeRequest req = new CreateGameTypeRequest();
        req.setName("Pool"); req.setDescription("d");

        assertThatThrownBy(() -> service.createGameType(req))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createGameType_derivesRulesEngineIdFromName_andDefaultsWinScore() {
        when(gameTypeRepository.findByName("Calcio Balilla")).thenReturn(Optional.empty());
        when(gameTypeRepository.save(any(GameType.class))).thenAnswer(inv -> { GameType g = inv.getArgument(0); g.setId("g1"); return g; });

        CreateGameTypeRequest req = new CreateGameTypeRequest();
        req.setName("Calcio Balilla"); req.setDescription("d"); req.setWinScoreTarget(0); // 0 => default 10

        service.createGameType(req);

        ArgumentCaptor<GameType> captor = ArgumentCaptor.forClass(GameType.class);
        verify(gameTypeRepository).save(captor.capture());
        assertThat(captor.getValue().getRulesEngineId()).isEqualTo("calcio_balilla");
        assertThat(captor.getValue().getWinScoreTarget()).isEqualTo(10);
    }

    // --- updateGameType ---
    @Test
    void updateGameType_doesNotRegenerateRulesEngineId() {
        GameType existing = gameType("g1", "Pool");
        existing.setRulesEngineId("pool"); // chiave stabile usata dal match-service
        when(gameTypeRepository.findById("g1")).thenReturn(Optional.of(existing));
        when(gameTypeRepository.findByName("Pool Deluxe")).thenReturn(Optional.empty());
        when(gameTypeRepository.save(any(GameType.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateGameTypeRequest req = new CreateGameTypeRequest();
        req.setName("Pool Deluxe"); req.setDescription("d2"); req.setWinScoreTarget(15);

        service.updateGameType("g1", req);

        ArgumentCaptor<GameType> captor = ArgumentCaptor.forClass(GameType.class);
        verify(gameTypeRepository).save(captor.capture());
        assertThat(captor.getValue().getRulesEngineId()).isEqualTo("pool"); // invariato
        assertThat(captor.getValue().getName()).isEqualTo("Pool Deluxe");
        assertThat(captor.getValue().getWinScoreTarget()).isEqualTo(15);
    }

    @Test
    void updateGameType_nameCollidesWithOther_conflict() {
        GameType existing = gameType("g1", "Pool");
        when(gameTypeRepository.findById("g1")).thenReturn(Optional.of(existing));
        when(gameTypeRepository.findByName("Darts")).thenReturn(Optional.of(gameType("g2", "Darts")));

        CreateGameTypeRequest req = new CreateGameTypeRequest();
        req.setName("Darts"); req.setDescription("d");

        assertThatThrownBy(() -> service.updateGameType("g1", req))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    // --- deleteSensor ---
    @Test
    void deleteSensor_notBelongingToGameType_badRequest() {
        GameType other = gameType("gOther", "X");
        SensorDefinition sensor = SensorDefinition.builder().id("s1").type("goal").gameType(other).build();
        when(sensorDefinitionRepository.findById("s1")).thenReturn(Optional.of(sensor));

        assertThatThrownBy(() -> service.deleteSensor("gWanted", "s1"))
                .isInstanceOf(BitpubException.class)
                .satisfies(e -> assertThat(((BitpubException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // --- addSensorToGameType ---
    @Test
    void addSensor_appliesDefaultsWhenZero() {
        GameType g = gameType("g1", "Pool");
        when(gameTypeRepository.findById("g1")).thenReturn(Optional.of(g));
        when(sensorDefinitionRepository.save(any(SensorDefinition.class))).thenAnswer(inv -> { SensorDefinition s = inv.getArgument(0); s.setId("s1"); return s; });

        AddSensorRequest req = new AddSensorRequest();
        req.setType("goal"); req.setDescription("d");
        req.setScoreIncrement(0); req.setSuccessProbability(0); // 0 => default 1 / 1.0

        service.addSensorToGameType("g1", req);

        ArgumentCaptor<SensorDefinition> captor = ArgumentCaptor.forClass(SensorDefinition.class);
        verify(sensorDefinitionRepository).save(captor.capture());
        assertThat(captor.getValue().getScoreIncrement()).isEqualTo(1);
        assertThat(captor.getValue().getSuccessProbability()).isEqualTo(1.0);
    }

    // --- deleteGameType ---
    @Test
    void deleteGameType_publishesEmptyRetainedConfig() {
        GameType g = gameType("g1", "Pool");
        when(gameTypeRepository.findById("g1")).thenReturn(Optional.of(g));

        service.deleteGameType("g1");

        verify(gameTypeRepository).delete(g);
        verify(configPublisher).publish(eq(""), any()); // payload vuoto = cancella retained
    }
}
