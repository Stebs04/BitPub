package it.uniupo.pissir.bitpub.matchservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.matchservice.domain.Match;
import it.uniupo.pissir.bitpub.matchservice.domain.SensorEventLog;
import it.uniupo.pissir.bitpub.matchservice.domain.Team;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.TeamResponseDto;
import it.uniupo.pissir.bitpub.matchservice.repository.MatchRepository;
import it.uniupo.pissir.bitpub.matchservice.repository.SensorEventLogRepository;
import it.uniupo.pissir.bitpub.matchservice.repository.TeamRepository;
import it.uniupo.pissir.bitpub.matchservice.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final SensorEventLogRepository sensorEventLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MatchDto startMatch(StartMatchRequestDto request) {
        // Verifica se c'è già un match in corso per quella gameInstance
        Optional<Match> existingMatch = matchRepository.findByGameInstanceIdAndStatus(request.getGameInstanceId(), "IN_PROGRESS");
        if (existingMatch.isPresent()) {
            throw new IllegalStateException("A match is already in progress for game instance: " + request.getGameInstanceId());
        }

        Match match = Match.builder()
                .gameInstanceId(request.getGameInstanceId())
                .gameTypeId(request.getGameTypeId())
                .status("IN_PROGRESS")
                .startTime(Instant.now())
                .build();

        Match savedMatch = matchRepository.save(match);

        List<Team> teams = request.getTeams().stream().map(t -> Team.builder()
                .name(t.getName())
                .playerIds(t.getPlayerIds())
                .match(savedMatch)
                .score(0)
                .build()).collect(Collectors.toList());

        teamRepository.saveAll(teams);
        savedMatch.setTeams(teams);

        return mapToDto(savedMatch);
    }

    @Override
    @Transactional
    public MatchDto endMatch(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));
        
        if ("COMPLETED".equals(match.getStatus())) {
            return mapToDto(match);
        }

        match.setStatus("COMPLETED");
        match.setEndTime(Instant.now());
        // Qui si potrebbe calcolare il resultPayload
        
        return mapToDto(matchRepository.save(match));
    }

    @Override
    public MatchDto getMatch(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));
        return mapToDto(match);
    }

    @Override
    @Transactional
    public void processSensorEvent(SensorEvent event) {
        String eventId = event.getEventId().toString();
        
        // Verifica Idempotenza
        if (sensorEventLogRepository.existsByEventId(eventId)) {
            log.warn("Event {} already processed, skipping.", eventId);
            return;
        }

        log.info("Processing event {} for gameInstanceId {}", eventId, event.getGameInstanceId());

        Optional<Match> activeMatchOpt = matchRepository.findByGameInstanceIdAndStatus(event.getGameInstanceId(), "IN_PROGRESS");
        
        Match match = null;
        if (activeMatchOpt.isPresent()) {
            match = activeMatchOpt.get();
            // Applica logica di punteggio base (es: per calciobalilla)
            if ("GOAL".equals(event.getSensorType()) && event.getPayload() != null && event.getPayload().containsKey("teamId")) {
                String teamId = event.getPayload().get("teamId").toString();
                match.getTeams().stream()
                        .filter(t -> t.getId().equals(teamId))
                        .findFirst()
                        .ifPresent(t -> t.setScore(t.getScore() + 1));
                matchRepository.save(match);
            }
        } else {
            log.info("No active match found for gameInstanceId {}. Event will just be logged.", event.getGameInstanceId());
        }

        String payloadStr = "{}";
        try {
            payloadStr = objectMapper.writeValueAsString(event.getPayload());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload", e);
        }

        SensorEventLog logEntry = SensorEventLog.builder()
                .eventId(eventId)
                .match(match)
                .sensorType(event.getSensorType())
                .timestamp(event.getTimestamp())
                .receivedAt(Instant.now())
                .payload(payloadStr)
                .build();
                
        sensorEventLogRepository.save(logEntry);
    }

    private MatchDto mapToDto(Match match) {
        List<TeamResponseDto> teamDtos = match.getTeams() == null ? List.of() : match.getTeams().stream()
                .map(t -> TeamResponseDto.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .playerIds(t.getPlayerIds())
                        .score(t.getScore())
                        .build())
                .collect(Collectors.toList());

        return MatchDto.builder()
                .id(match.getId())
                .gameInstanceId(match.getGameInstanceId())
                .gameTypeId(match.getGameTypeId())
                .status(match.getStatus())
                .startTime(match.getStartTime())
                .endTime(match.getEndTime())
                .teams(teamDtos)
                .resultPayload(match.getResultPayload())
                .build();
    }
}
