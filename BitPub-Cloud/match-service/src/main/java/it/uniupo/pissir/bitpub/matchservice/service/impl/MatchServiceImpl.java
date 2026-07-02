package it.uniupo.pissir.bitpub.matchservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
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
import it.uniupo.pissir.bitpub.matchservice.dto.GameStateDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.integration.mqtt.support.MqttHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("mqttOutboundChannel")
    private MessageChannel mqttOutboundChannel;

    @Value("${statistics.service.url:http://localhost:8087}")
    private String statisticsServiceUrl;

    @Value("${user.service.url:http://localhost:8082}")
    private String userServiceUrl;

    @Value("${locale.service.url:http://localhost:8083}")
    private String localeServiceUrl;

    /**
     * Resolves the owning locale for a gameInstance by calling locale-service.
     * Needed to scope LOCALE_ADMIN access to matches of their own locale.
     */
    private String resolveLocaleId(String gameInstanceId) {
        try {
            Map response = RestClient.create(localeServiceUrl)
                    .get()
                    .uri("/api/v1/locales/games/{id}", gameInstanceId)
                    .retrieve()
                    .body(Map.class);
            return response != null && response.containsKey("localeId") ? response.get("localeId").toString() : null;
        } catch (Exception e) {
            log.error("Failed to resolve localeId for gameInstanceId: {}", gameInstanceId, e);
            return null;
        }
    }

    /**
     * A LOCALE_ADMIN may only access matches of the locale they are assigned to.
     * PLATFORM_ADMIN and other roles are left unrestricted (read-only monitoring is not
     * gated for players/other admins, only LOCALE_ADMIN is scoped down).
     */
    public void assertMatchLocaleAccess(String matchLocaleId, String callerId, String callerRole) {
        if (!"LOCALE_ADMIN".equals(callerRole)) {
            return;
        }
        String adminLocaleId = resolveAdminLocaleId(callerId);
        if (adminLocaleId == null || !adminLocaleId.equals(matchLocaleId)) {
            throw new BitpubException("LOCALE_ADMIN can only access matches of their own locale", HttpStatus.FORBIDDEN);
        }
    }

    /** Returns the localeId of the locale owned by the given adminId, or null if none. */
    public String resolveAdminLocaleId(String adminId) {
        if (adminId == null) {
            return null;
        }
        try {
            List response = RestClient.create(localeServiceUrl)
                    .get()
                    .uri("/api/v1/locales/by-admin/{adminId}", adminId)
                    .retrieve()
                    .body(List.class);
            if (response != null && !response.isEmpty() && response.get(0) instanceof Map) {
                Object id = ((Map) response.get(0)).get("id");
                return id != null ? id.toString() : null;
            }
        } catch (Exception e) {
            log.error("Failed to resolve locale for adminId: {}", adminId, e);
        }
        return null;
    }

    private String ensureUser(String username) {
        try {
            Map<String, String> request = Map.of("username", username);
            String body = objectMapper.writeValueAsString(request);
            Map response = RestClient.create(userServiceUrl)
                    .post()
                    .uri("/api/v1/users/ensure")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.containsKey("id")) {
                return response.get("id").toString();
            }
        } catch (Exception e) {
            log.error("Failed to ensure user in user-service for username: {}", username, e);
        }
        return null;
    }

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
                .localeId(resolveLocaleId(request.getGameInstanceId()))
                .gameTypeId(request.getGameTypeId())
                .status("IN_PROGRESS")
                .startTime(Instant.now())
                .build();

        Match savedMatch = matchRepository.save(match);

        List<Team> teams = request.getTeams().stream().map(t -> {
            List<String> playerIds = new ArrayList<>();
            if (t.getName() != null) {
                String userId = ensureUser(t.getName());
                if (userId != null) {
                    playerIds.add(userId);
                }
            }
            return Team.builder()
                .name(t.getName())
                .playerIds(playerIds)
                .match(savedMatch)
                .score(0)
                .build();
        }).collect(Collectors.toList());

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
        Match saved = matchRepository.save(match);

        // Notify statistics service with the match result
        notifyStatisticsService(saved);

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDto getMatch(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));
        return mapToDto(match);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchDto> getActiveMatches() {
        return matchRepository.findByStatus("IN_PROGRESS").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchDto> getActiveMatchesByLocale(String localeId) {
        return matchRepository.findByLocaleIdAndStatus(localeId, "IN_PROGRESS").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Player match history — used by the dashboard/stats views. No repository finder
     * exists for team playerIds (element-collection), so filter in-memory.
     */
    @Transactional(readOnly = true)
    public List<MatchDto> getMatchesByPlayer(String playerId) {
        return matchRepository.findAll().stream()
                .filter(m -> m.getTeams() != null && m.getTeams().stream()
                        .anyMatch(t -> t.getPlayerIds() != null && t.getPlayerIds().contains(playerId)))
                .sorted(Comparator.comparing(Match::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToDto)
                .collect(Collectors.toList());
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
        if (activeMatchOpt.isEmpty() && "MATCH_START".equals(event.getSensorType())) {
            match = Match.builder()
                .gameInstanceId(event.getGameInstanceId())
                .localeId(resolveLocaleId(event.getGameInstanceId()))
                .gameTypeId(event.getGameInstanceId().contains("-") ? event.getGameInstanceId().split("-")[0] : "unknown")
                .status("IN_PROGRESS")
                .startTime(Instant.now())
                .teams(new ArrayList<>())
                .build();
            match = matchRepository.save(match);

            // Extract player names from MQTT payload if provided by the simulator
            String teamAName = "RED";
            String teamBName = "BLUE";
            if (event.getPayload() != null) {
                if (event.getPayload().containsKey("teamAName")) teamAName = event.getPayload().get("teamAName").toString();
                if (event.getPayload().containsKey("teamBName")) teamBName = event.getPayload().get("teamBName").toString();
            }

            String userAId = ensureUser(teamAName);
            String userBId = ensureUser(teamBName);

            match.getTeams().add(Team.builder()
                .name(teamAName)
                .playerIds(userAId != null ? new ArrayList<>(List.of(userAId)) : new ArrayList<>())
                .score(0)
                .match(match)
                .build());
            match.getTeams().add(Team.builder()
                .name(teamBName)
                .playerIds(userBId != null ? new ArrayList<>(List.of(userBId)) : new ArrayList<>())
                .score(0)
                .match(match)
                .build());

            teamRepository.saveAll(match.getTeams());
            activeMatchOpt = Optional.of(match);
            log.info("Auto-created match {} for gameInstanceId {}", match.getId(), match.getGameInstanceId());
        }

        if (activeMatchOpt.isPresent()) {
            match = activeMatchOpt.get();

            // Apply scoring logic per game type
            if ("GOAL".equals(event.getSensorType()) && event.getPayload() != null && event.getPayload().containsKey("team")) {
                String teamName = event.getPayload().get("team").toString();
                match.getTeams().stream()
                        .filter(t -> t.getName().equalsIgnoreCase(teamName))
                        .findFirst()
                        .ifPresent(t -> t.setScore(t.getScore() + 1));
                matchRepository.save(match);
            }
            if ("DART_HIT".equals(event.getSensorType()) && event.getPayload() != null && event.getPayload().containsKey("score")) {
                int score = Integer.parseInt(event.getPayload().get("score").toString());
                int multiplier = event.getPayload().containsKey("multiplier") ? Integer.parseInt(event.getPayload().get("multiplier").toString()) : 1;
                // Add to the active player / team A for darts
                match.getTeams().stream().findFirst().ifPresent(t -> t.setScore(t.getScore() + (score * multiplier)));
                matchRepository.save(match);
            }
            if ("BALL_POCKETED".equals(event.getSensorType())) {
                match.getTeams().stream().findFirst().ifPresent(t -> t.setScore(t.getScore() + 1));
                matchRepository.save(match);
            }
            if ("FOUL".equals(event.getSensorType())) {
                // A foul costs 1 point from team A / current player
                match.getTeams().stream().findFirst().ifPresent(t -> t.setScore(Math.max(0, t.getScore() - 1)));
                matchRepository.save(match);
            }
            if ("MATCH_END".equals(event.getSensorType())) {
                match.setStatus("COMPLETED");
                match.setEndTime(Instant.now());
                matchRepository.save(match);
                // Notify statistics service about match result to update leaderboard
                notifyStatisticsService(match);
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

        // Publish updated game state to MQTT so the Kiosk gets it in real-time
        if (match != null) {
            publishGameState(match, event);
        }
    }

    /**
     * Determines the winner team (highest score) and notifies the statistics-service
     * to update the leaderboard for both players.
     */
    private void notifyStatisticsService(Match match) {
        if (match.getTeams() == null || match.getTeams().isEmpty()) return;
        try {
            Team winner = match.getTeams().stream()
                    .max(Comparator.comparingInt(Team::getScore))
                    .orElse(null);
            Team loser = match.getTeams().stream()
                    .filter(t -> winner == null || !t.getId().equals(winner.getId()))
                    .findFirst().orElse(null);

            String winnerName = winner != null ? winner.getName() : "Unknown";
            String loserName  = loser  != null ? loser.getName()  : "Unknown";
            int winnerScore   = winner != null ? winner.getScore() : 0;
            int loserScore    = loser  != null ? loser.getScore()  : 0;

            Map<String, Object> resultEvent = new java.util.HashMap<>();
            resultEvent.put("gameTypeId", match.getGameTypeId());
            resultEvent.put("winnerName", winnerName);
            resultEvent.put("loserName", loserName);
            resultEvent.put("winnerScore", winnerScore);
            resultEvent.put("loserScore", loserScore);
            resultEvent.put("matchId", match.getId());
            resultEvent.put("localeId", match.getLocaleId());

            String body = objectMapper.writeValueAsString(resultEvent);
            RestClient.create(statisticsServiceUrl)
                    .post()
                    .uri("/api/v1/statistics/match-result")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notified statistics-service: winner={}, loser={}, gameType={}", winnerName, loserName, match.getGameTypeId());
        } catch (Exception e) {
            log.error("Failed to notify statistics-service", e);
        }
    }

    private void publishGameState(Match match, SensorEvent event) {
        String teamAName = "RED";
        String teamBName = "BLUE";
        int scoreA = 0;
        int scoreB = 0;
        if (match.getTeams() != null && !match.getTeams().isEmpty()) {
            teamAName = match.getTeams().get(0).getName();
            scoreA    = match.getTeams().get(0).getScore();
            if (match.getTeams().size() > 1) {
                teamBName = match.getTeams().get(1).getName();
                scoreB    = match.getTeams().get(1).getScore();
            }
        }
        String status = "IN_PROGRESS".equals(match.getStatus()) ? "PLAYING" : "FINISHED";

        // Determine winner name on match end
        String winnerName = null;
        if ("FINISHED".equals(status) && match.getTeams() != null && !match.getTeams().isEmpty()) {
            winnerName = match.getTeams().stream()
                    .max(Comparator.comparingInt(Team::getScore))
                    .map(Team::getName).orElse(null);
        }

        GameStateDto stateDto = GameStateDto.builder()
                .matchId(match.getId())
                .gameTypeId(match.getGameTypeId())
                .status(status)
                .teamAName(teamAName)
                .teamBName(teamBName)
                .scoreTeamA(scoreA)
                .scoreTeamB(scoreB)
                .timeRemainingSeconds(0)
                .currentEventMessage(event.getSensorType())
                .winnerName(winnerName)
                .build();

        try {
            String statePayload = objectMapper.writeValueAsString(stateDto);
            String topic = "bitpub/match/LOC-1/" + match.getGameInstanceId() + "/state";
            mqttOutboundChannel.send(MessageBuilder.withPayload(statePayload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.RETAINED, true)
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish GameStateDto", e);
        }
    }

    private MatchDto mapToDto(Match match) {
        List<TeamResponseDto> teamDtos = match.getTeams() == null ? List.of() : match.getTeams().stream()
                .map(t -> TeamResponseDto.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .playerIds(t.getPlayerIds() != null ? new java.util.ArrayList<>(t.getPlayerIds()) : new java.util.ArrayList<>())
                        .score(t.getScore())
                        .build())
                .collect(Collectors.toList());

        return MatchDto.builder()
                .id(match.getId())
                .gameInstanceId(match.getGameInstanceId())
                .localeId(match.getLocaleId())
                .gameTypeId(match.getGameTypeId())
                .status(match.getStatus())
                .startTime(match.getStartTime())
                .endTime(match.getEndTime())
                .teams(teamDtos)
                .resultPayload(match.getResultPayload())
                .build();
    }
}
