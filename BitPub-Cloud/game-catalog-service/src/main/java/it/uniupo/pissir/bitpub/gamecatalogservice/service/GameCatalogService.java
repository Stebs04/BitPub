/**
 * Autore: Stefano Bellan Matricola 20054330
 */
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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
     * Routine di allineamento eseguita al completamento dell'avvio dell'applicazione.
     * Riemette le configurazioni di tutti i giochi presenti a database sul broker MQTT: questa operazione
     * mitiga la perdita dei messaggi 'retained' qualora il broker venisse riavviato, garantendo ai simulatori
     * il ripristino immediato delle regole applicative correnti.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void republishAllConfigsOnStartup() {
        List<GameTypeDto> gameTypes = getAllGameTypes();
        log.info("Republishing {} game configs to MQTT on startup", gameTypes.size());
        gameTypes.forEach(dto -> publishConfig(dto.getId()));
    }

    /**
     * Notifica l'intero ecosistema (i.e. simulatori e nodi fisici) dei parametri attuali associati a una disciplina.
     * Implementa un meccanismo 'fire-and-forget': eventuali indisponibilità della rete MQTT non compromettono
     * la corretta persistenza relazionale delle informazioni.
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
                // Derivazione deterministica dell'identificativo strategico dal nome del gioco.
                // Tale chiave viene sfruttata dal match-service per istanziare il corretto motore logico.
                .rulesEngineId(request.getName().trim().toLowerCase().replaceAll("\\s+", "_"))
                .winScoreTarget(request.getWinScoreTarget() > 0 ? request.getWinScoreTarget() : 10)
                .sensors(new ArrayList<>())
                .build();

        GameType saved = gameTypeRepository.save(gameType);
        publishConfig(saved.getId());
        return mapToDto(saved);
    }

    /**
     * Aggiorna le specifiche anagrafiche della tipologia di gioco.
     * Il campo rulesEngineId viene deliberatamente preservato per garantire la compatibilità retroattiva
     * con i motori di simulazione operanti sui match attualmente in corso.
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
     * Dissocia e sopprime in modo permanente la definizione hardware di un sensore legata a un determinato gioco.
     * Viene garantito a monte il controllo dell'effettiva affiliazione logica tra le due entità.
     */
    @Transactional
    public void deleteSensor(String gameTypeId, String sensorId) {
        SensorDefinition sensor = sensorDefinitionRepository.findById(sensorId)
                .orElseThrow(() -> new ResourceNotFoundException("SensorDefinition", "id", sensorId));

        if (sensor.getGameType() == null || !gameTypeId.equals(sensor.getGameType().getId())) {
            throw new BitpubException("Sensor does not belong to the specified GameType", HttpStatus.BAD_REQUEST);
        }

        // Rimozione sincronizzata dal contesto di persistenza (collezione in memoria) seguita da flush immediato.
        // Evita letture sporche (dirty read) durante la pubblicazione del nuovo stato via MQTT.
        GameType gameType = sensor.getGameType();
        if (gameType.getSensors() != null) {
            gameType.getSensors().remove(sensor);
        }
        sensorDefinitionRepository.delete(sensor);
        sensorDefinitionRepository.flush();
        publishConfig(gameTypeId);
    }

    /**
     * Purga una tipologia di gioco dal database relazionale.
     * Procede parimenti alla rimozione del messaggio MQTT 'retained' inviando un payload vuoto, 
     * scongiurando così il provisioning di dati obsoleti ai nuovi nodi in ascolto.
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

        // Allineamento della cache di sessione per includere il nuovo sensore prima della sincronizzazione.
        // Il flush assicura l'estrazione aggiornata per il corretto invio del payload configurativo.
        if (gameType.getSensors() != null) {
            gameType.getSensors().add(saved);
        } else {
            gameType.setSensors(new ArrayList<>(List.of(saved)));
        }
        sensorDefinitionRepository.flush();

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
