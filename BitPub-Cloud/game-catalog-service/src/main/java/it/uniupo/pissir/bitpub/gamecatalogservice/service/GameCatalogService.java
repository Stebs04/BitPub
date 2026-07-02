package it.uniupo.pissir.bitpub.gamecatalogservice.service;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.gamecatalogservice.domain.GameType;
import it.uniupo.pissir.bitpub.gamecatalogservice.domain.SensorDefinition;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.AddSensorRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.CreateGameTypeRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.GameTypeDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.SensorDefinitionDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.GameTypeRepository;
import it.uniupo.pissir.bitpub.gamecatalogservice.repository.SensorDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameCatalogService {

    private final GameTypeRepository gameTypeRepository;
    private final SensorDefinitionRepository sensorDefinitionRepository;

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
                .sensors(new ArrayList<>())
                .build();

        GameType saved = gameTypeRepository.save(gameType);
        return mapToDto(saved);
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
                .gameType(gameType)
                .build();

        SensorDefinition saved = sensorDefinitionRepository.save(sensor);
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
                .build();
    }
}
