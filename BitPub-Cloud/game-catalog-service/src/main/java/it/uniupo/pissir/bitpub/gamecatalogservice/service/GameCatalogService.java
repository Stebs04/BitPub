package it.uniupo.pissir.bitpub.gamecatalogservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.gamecatalogservice.config.MqttConfig.ConfigPublisher;
import it.uniupo.pissir.bitpub.gamecatalogservice.domain.GameType;
import it.uniupo.pissir.bitpub.gamecatalogservice.domain.SensorDefinition;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.AddSensorRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.CreateGameTypeRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.GameTypeDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.SensorDefinitionDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.GameTypeRepository;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.SensorDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameCatalogService {

    private final GameTypeRepository gameTypeRepository;
    private final SensorDefinitionRepository sensorDefinitionRepository;
    private final ConfigPublisher configPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Pubblica lo snapshot completo (GameType + Sensori) su bitpub/config/games/{id} (retained),
     * cosi' i simulatori aggiornano le regole in cache. Fire-and-forget: un errore MQTT non deve
     * far fallire la mutazione del catalogo.
     */
    public void publishConfig(String gameTypeId) {
        try {
            GameTypeDto dto = getGameTypeById(gameTypeId);
            configPublisher.publish(objectMapper.writeValueAsString(dto), MqttTopics.getGameConfigTopic(gameTypeId));
            log.info("Published game config for gameTypeId={} ({} sensors)", gameTypeId,
                    dto.getSensors() != null ? dto.getSensors().size() : 0);
        } catch (Exception e) {
            log.error("Failed to publish game config for gameTypeId={}", gameTypeId, e);
        }
    }

    @Transactional
    public GameTypeDto createGameType(CreateGameTypeRequest request) {
        if (gameTypeRepository.findByName(request.getName()).isPresent()) {
            throw new BitpubException("GameType with this name already exists", HttpStatus.CONFLICT);
        }

        GameType gameType = GameType.builder()
                .name(request.getName())
                .description(request.getDescription())
                // Derived from name since rulesEngineId is not part of the create request;
                // match-service's Strategy lookup uses this key (e.g. "Calciobalilla" -> "calciobalilla").
                .rulesEngineId(request.getName().trim().toLowerCase().replaceAll("\\s+", "_"))
                .winScoreTarget(request.getWinScoreTarget() > 0 ? request.getWinScoreTarget() : 10)
                .sensors(new ArrayList<>())
                .build();

        GameType saved = gameTypeRepository.save(gameType);
        publishConfig(saved.getId());
        return mapToDto(saved);
    }

    /**
     * Modifica nome/descrizione di un tipo di gioco. Il rulesEngineId NON viene rigenerato:
     * e' la chiave usata dal match-service per selezionare la Strategy e cambiarlo
     * desincronizzerebbe le partite gia' collegate a questo GameType.
     */
    @Transactional
    public GameTypeDto updateGameType(String id, CreateGameTypeRequest request) {
        GameType gameType = gameTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GameType", "id", id));

        if (!gameType.getName().equals(request.getName())
                && gameTypeRepository.findByName(request.getName()).isPresent()) {
            throw new BitpubException("GameType with this name already exists", HttpStatus.CONFLICT);
        }

        gameType.setName(request.getName());
        gameType.setDescription(request.getDescription());
        if (request.getWinScoreTarget() > 0) {
            gameType.setWinScoreTarget(request.getWinScoreTarget());
        }

        GameType saved = gameTypeRepository.save(gameType);
        publishConfig(saved.getId());
        return mapToDto(saved);
    }

    /**
     * Rimuove un evento simulato (SensorDefinition) da un tipo di gioco.
     * Verifica che il sensore appartenga davvero al GameType indicato.
     */
    @Transactional
    public void deleteSensor(String gameTypeId, String sensorId) {
        SensorDefinition sensor = sensorDefinitionRepository.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("SensorDefinition", "id", sensorId));

        if (sensor.getGameType() == null || !gameTypeId.equals(sensor.getGameType().getId())) {
            throw new BitpubException("Sensor does not belong to the specified GameType", HttpStatus.BAD_REQUEST);
        }

        // Rimuovi dalla collezione in memoria + flush: publishConfig rilegge il GameType nella
        // stessa transazione e senza questo vedrebbe il sensore appena cancellato (dirty read).
        GameType gameType = sensor.getGameType();
        if (gameType.getSensors() != null) {
            gameType.getSensors().remove(sensor);
        }
        sensorDefinitionRepository.delete(sensor);
        sensorDefinitionRepository.flush();
        publishConfig(gameTypeId);
    }

    /**
     * Rimuove un tipo di gioco dal catalogo e pulisce la config retained sull'edge
     * pubblicando un payload vuoto su bitpub/config/games/{id} (cancella il messaggio retained).
     */
    @Transactional
    public void deleteGameType(String id) {
        GameType gameType = gameTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GameType", "id", id));

        gameTypeRepository.delete(gameType);
        configPublisher.publish("", MqttTopics.getGameConfigTopic(id));
    }

    public GameTypeDto getGameTypeById(String id) {
        GameType gameType = gameTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GameType", "id", id));
        return mapToDto(gameType);
    }

    public List<GameTypeDto> getAllGameTypes() {
        return gameTypeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SensorDefinitionDto addSensorToGameType(String gameTypeId, AddSensorRequest request) {
        GameType gameType = gameTypeRepository.findById(gameTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("GameType", "id", gameTypeId));

        SensorDefinition sensor = SensorDefinition.builder()
                .type(request.getType())
                .description(request.getDescription())
                .isActuator(request.isActuator())
                .scoreIncrement(request.getScoreIncrement() != 0 ? request.getScoreIncrement() : 1)
                .successProbability(request.getSuccessProbability() > 0 ? request.getSuccessProbability() : 1.0)
                .gameType(gameType)
                .build();

        SensorDefinition saved = sensorDefinitionRepository.save(sensor);
        publishConfig(gameTypeId);
        return mapSensorToDto(saved);
    }

    public List<SensorDefinitionDto> getSensorsByGameType(String gameTypeId) {
        return sensorDefinitionRepository.findByGameTypeId(gameTypeId).stream()
                .map(this::mapSensorToDto)
                .collect(Collectors.toList());
    }

    private GameTypeDto mapToDto(GameType gameType) {
        return GameTypeDto.builder()
                .id(gameType.getId())
                .name(gameType.getName())
                .description(gameType.getDescription())
                .winScoreTarget(gameType.getWinScoreTarget())
                .sensors(gameType.getSensors() != null ? 
                        gameType.getSensors().stream().map(this::mapSensorToDto).collect(Collectors.toList()) : 
                        new ArrayList<>())
                .build();
    }

    private SensorDefinitionDto mapSensorToDto(SensorDefinition sensor) {
        return SensorDefinitionDto.builder()
                .id(sensor.getId())
                .type(sensor.getType())
                .description(sensor.getDescription())
                .isActuator(sensor.isActuator())
                .scoreIncrement(sensor.getScoreIncrement())
                .successProbability(sensor.getSuccessProbability())
                .build();
    }
}
