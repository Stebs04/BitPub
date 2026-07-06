package it.uniupo.pissir.bitpub.matchservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.matchservice.domain.Match;
import it.uniupo.pissir.bitpub.matchservice.domain.SensorEventLog;
import it.uniupo.pissir.bitpub.matchservice.domain.Team;
import it.uniupo.pissir.bitpub.matchservice.dto.GameActionRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.JoinLobbyRequestDto;
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
import java.util.concurrent.ThreadLocalRandom;
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
        Map info = fetchGameInstanceInfo(gameInstanceId);
        return info != null && info.containsKey("localeId") ? info.get("localeId").toString() : null;
    }

    /**
     * Fetches the full GameInstanceDto (localeId, gameTypeId, active) from locale-service.
     * Used by the PLAYER matchmaking flow to validate the machine is switched on and to
     * know which gameTypeId/localeId to stamp on the lobby match.
     */
    private Map fetchGameInstanceInfo(String gameInstanceId) {
        try {
            return RestClient.create(localeServiceUrl)
                    .get()
                    .uri("/api/v1/locales/games/{id}", gameInstanceId)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("Failed to resolve gameInstance info for gameInstanceId: {}", gameInstanceId, e);
            return null;
        }
    }

    /**
     * A LOCALE_ADMIN may only access matches of the locale they are assigned to.
     * PLATFORM_ADMIN and other roles are left unrestricted (read-only monitoring is not
     * gated for players/other admins, only LOCALE_ADMIN is scoped down).
     * Prefers the locale claim forwarded by the gateway (callerLocaleId); falls back to
     * resolving it via locale-service if the caller's token predates the claim.
     */
    public void assertMatchLocaleAccess(String matchLocaleId, String callerId, String callerRole, String callerLocaleId) {
        if (!"LOCALE_ADMIN".equals(callerRole)) {
            return;
        }
        String adminLocaleId = callerLocaleId != null ? callerLocaleId : resolveAdminLocaleId(callerId);
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
        Optional<Match> existingMatch = matchRepository.findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc(request.getGameInstanceId(), "IN_PROGRESS");
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
        match.setCurrentTurnUserId(null);
        match.setTeamBased(isTeamBased(match));
        Match saved = matchRepository.save(match);

        // Il vincitore e' calcolato da winnerTeam() in base al punteggio piu' alto
        // (piu' basso a freccette, dove si parte da 501 e si scende a 0).
        notifyStatisticsService(saved);
        publishGameState(saved, "FINISHED", "MATCH_END");

        return mapToDto(saved);
    }

    /**
     * PLAYER matchmaking: se esiste gia' una lobby WAITING_FOR_PLAYERS su questa gameInstance,
     * vi aggiunge il chiamante come secondo giocatore e la porta IN_PROGRESS (match "STARTED"),
     * pubblicando lo stato aggiornato via MQTT cosi' che entrambi i client vedano la transizione
     * in tempo reale. Altrimenti crea una nuova lobby in attesa del secondo giocatore.
     */
    @Override
    @Transactional
    public MatchDto joinLobby(JoinLobbyRequestDto request, String playerId) {
        String gameInstanceId = request.getGameInstanceId();
        String username = request.getUsername();

        Optional<Match> waiting = matchRepository.findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc(gameInstanceId, "WAITING_FOR_PLAYERS");

        if (waiting.isPresent()) {
            Match match = waiting.get();

            // Reconnect idempotente: lo stesso giocatore che ripolla/riapre la pagina non deve duplicarsi in team.
            boolean alreadyIn = match.getTeams() != null && match.getTeams().stream()
                    .anyMatch(t -> t.getPlayerIds() != null && t.getPlayerIds().contains(playerId));
            if (alreadyIn) {
                return mapToDto(match);
            }

            Team secondTeam = Team.builder()
                    .name(username)
                    .playerIds(new ArrayList<>(List.of(playerId)))
                    .score(0)
                    .match(match)
                    .build();
            teamRepository.save(secondTeam);
            match.getTeams().add(secondTeam);

            match.setStatus("IN_PROGRESS");
            match.setStartTime(Instant.now());
            initializeGameStart(match);
            Match saved = matchRepository.save(match);

            publishLobbyState(saved, "MATCH_START");
            log.info("Lobby {} STARTED: {} joined gameInstanceId {}", saved.getId(), username, gameInstanceId);
            return mapToDto(saved);
        }

        // Nessuna lobby in attesa: verifica che la gameInstance sia una macchina attiva del locale.
        Map info = fetchGameInstanceInfo(gameInstanceId);
        boolean active = info != null && Boolean.TRUE.equals(info.get("active"));
        if (info == null || !active) {
            throw new BitpubException("Il gioco selezionato non e' attivo in questo momento", HttpStatus.CONFLICT);
        }
        String localeId = info.get("localeId") != null ? info.get("localeId").toString() : null;
        String gameTypeId = info.get("gameTypeId") != null ? info.get("gameTypeId").toString() : "unknown";

        Match match = Match.builder()
                .gameInstanceId(gameInstanceId)
                .localeId(localeId)
                .gameTypeId(gameTypeId)
                .status("WAITING_FOR_PLAYERS")
                .teams(new ArrayList<>())
                .build();
        Match saved = matchRepository.save(match);

        Team firstTeam = Team.builder()
                .name(username)
                .playerIds(new ArrayList<>(List.of(playerId)))
                .score(0)
                .match(saved)
                .build();
        teamRepository.save(firstTeam);
        saved.getTeams().add(firstTeam);

        publishLobbyState(saved, "WAITING_FOR_PLAYERS");
        log.info("Lobby {} created WAITING_FOR_PLAYERS by {} on gameInstanceId {}", saved.getId(), username, gameInstanceId);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchDto> getWaitingLobby(String gameInstanceId) {
        return matchRepository.findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc(gameInstanceId, "WAITING_FOR_PLAYERS")
                .map(this::mapToDto);
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

        Optional<Match> activeMatchOpt = matchRepository.findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc(event.getGameInstanceId(), "IN_PROGRESS");

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

    // ── Gameplay interattivo a turni ────────────────────────────────────────────

    private enum GameKind { FOOSBALL, BILLIARDS, DARTS, UNKNOWN }

    private static final int FOOSBALL_TARGET = 10;  // goal necessari per vincere a calciobalilla
    private static final int BILLIARDS_TARGET = 7;   // palle del proprio gruppo da imbucare prima della boccia degli 8
    private static final int DARTS_START = 501;      // punteggio iniziale a freccette (regola ufficiale "501 double-out")
    private static final int DARTS_THROWS_PER_TURN = 3;

    private static final double FOOSBALL_GOAL_CHANCE = 0.30;   // 30% goal, 70% parata/fuori
    private static final double BILLIARDS_POCKET_CHANCE = 0.50; // 50% imbuca

    private GameKind classify(String gameTypeId) {
        String g = gameTypeId == null ? "" : gameTypeId.toLowerCase();
        if (g.contains("calcio") || g.contains("foosball") || g.contains("balilla")) return GameKind.FOOSBALL;
        if (g.contains("biliard") || g.contains("billiard") || g.contains("pool")) return GameKind.BILLIARDS;
        if (g.contains("frecc") || g.contains("dart")) return GameKind.DARTS;
        return GameKind.UNKNOWN;
    }

    /** Partita a squadre se almeno un team ha piu' di un giocatore; altrimenti individuale. */
    private boolean isTeamBased(Match match) {
        return match.getTeams() != null && match.getTeams().stream()
                .anyMatch(t -> t.getPlayerIds() != null && t.getPlayerIds().size() > 1);
    }

    private String firstPlayerId(Team team) {
        return team != null && team.getPlayerIds() != null && !team.getPlayerIds().isEmpty()
                ? team.getPlayerIds().get(0) : null;
    }

    /**
     * Allo START del match assegna casualmente il primo turno e inizializza lo stato specifico
     * del gioco (punteggio 501 a freccette, spaccata non ancora avvenuta a biliardo).
     */
    private void initializeGameStart(Match match) {
        List<Team> teams = match.getTeams();
        if (teams == null || teams.size() < 2) return;

        Team starter = ThreadLocalRandom.current().nextBoolean() ? teams.get(0) : teams.get(1);
        match.setCurrentTurnUserId(firstPlayerId(starter));
        match.setBreakDone(false);
        match.setSolidTeamId(null);
        match.setStripedTeamId(null);
        match.setThrowsInTurn(0);

        if (classify(match.getGameTypeId()) == GameKind.DARTS) {
            teams.forEach(t -> t.setScore(DARTS_START));
            teamRepository.saveAll(teams);
        }
    }

    @Override
    @Transactional
    public MatchDto processGameAction(String matchId, String playerId, GameActionRequestDto action) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));

        if (!"IN_PROGRESS".equals(match.getStatus())) {
            throw new BitpubException("La partita non e' in corso", HttpStatus.CONFLICT);
        }
        if (match.getCurrentTurnUserId() == null || !match.getCurrentTurnUserId().equals(playerId)) {
            throw new BitpubException("Non e' il tuo turno", HttpStatus.FORBIDDEN);
        }

        List<Team> teams = match.getTeams();
        if (teams == null || teams.size() < 2) {
            throw new BitpubException("Partita senza due giocatori", HttpStatus.CONFLICT);
        }

        Team current = teams.stream()
                .filter(t -> t.getPlayerIds() != null && t.getPlayerIds().contains(playerId))
                .findFirst()
                .orElseThrow(() -> new BitpubException("Giocatore non nel match", HttpStatus.FORBIDDEN));
        Team opponent = teams.stream().filter(t -> !t.getId().equals(current.getId())).findFirst().orElse(null);

        String eventMessage;
        boolean finished = false;

        switch (classify(match.getGameTypeId())) {
            case FOOSBALL: {
                boolean goal = ThreadLocalRandom.current().nextDouble() < FOOSBALL_GOAL_CHANCE;
                if (goal) {
                    current.setScore(current.getScore() + 1);
                    eventMessage = "GOAL";
                    if (current.getScore() >= FOOSBALL_TARGET) finished = true;
                } else {
                    eventMessage = "SAVE";
                }
                // Il turno passa sempre all'avversario dopo il tiro.
                if (!finished) match.setCurrentTurnUserId(firstPlayerId(opponent));
                break;
            }
            case BILLIARDS: {
                if (!match.isBreakDone()) {
                    // Spaccata: assegna casualmente Piene/Spezzate ai due team.
                    boolean currentGetsSolid = ThreadLocalRandom.current().nextBoolean();
                    match.setSolidTeamId(currentGetsSolid ? current.getId() : opponent.getId());
                    match.setStripedTeamId(currentGetsSolid ? opponent.getId() : current.getId());
                    match.setBreakDone(true);
                    eventMessage = "BREAK";
                    // Dopo la spaccata il turno resta a chi ha spaccato.
                } else if (current.getScore() >= BILLIARDS_TARGET) {
                    // Regola 8-ball: dopo aver imbucato tutte le palle del proprio gruppo (7),
                    // il colpo successivo deve imbucare la boccia nera (8) per vincere la partita.
                    // Un tiro mancato qui e' semplicemente un errore normale (passa il turno);
                    // imbucare la boccia nera prima di aver completato il gruppo non e' modellabile
                    // con l'azione generica "SHOOT" attuale, quindi si applica solo a gruppo completo.
                    boolean pocketedEightBall = ThreadLocalRandom.current().nextDouble() < BILLIARDS_POCKET_CHANCE;
                    if (pocketedEightBall) {
                        eventMessage = "BALL_POCKETED";
                        finished = true;
                    } else {
                        eventMessage = "MISS";
                        match.setCurrentTurnUserId(firstPlayerId(opponent));
                    }
                } else {
                    boolean pocket = ThreadLocalRandom.current().nextDouble() < BILLIARDS_POCKET_CHANCE;
                    if (pocket) {
                        current.setScore(current.getScore() + 1);
                        eventMessage = "BALL_POCKETED";
                        // Imbuca: mantiene il turno. Il gruppo si completa a BILLIARDS_TARGET,
                        // ma la vittoria arriva solo imbucando poi la boccia nera (ramo sopra).
                    } else {
                        eventMessage = "MISS";
                        match.setCurrentTurnUserId(firstPlayerId(opponent));
                    }
                }
                break;
            }
            case DARTS: {
                // Regola ufficiale "501 double-out": si chiude solo con un doppio (o il Bull, 50).
                // Bust (tiro annullato, punteggio invariato) se si va sotto 0, si resta a 1
                // (impossibile chiudere da 1 con un doppio), o si arriva a 0 senza un doppio.
                int sector = action.getSector() != null ? action.getSector() : 0;
                int multiplier = action.getMultiplier() != null ? action.getMultiplier() : 1;
                int points = sector * multiplier;
                int remaining = current.getScore() - points;
                boolean isDoubleFinish = multiplier == 2 || sector == 50;
                if (remaining < 0 || remaining == 1 || (remaining == 0 && !isDoubleFinish)) {
                    eventMessage = "FOUL";
                } else {
                    current.setScore(remaining);
                    eventMessage = "DART_HIT";
                    if (remaining == 0) finished = true;
                }
                if (!finished) {
                    match.setThrowsInTurn(match.getThrowsInTurn() + 1);
                    if (match.getThrowsInTurn() >= DARTS_THROWS_PER_TURN) {
                        match.setThrowsInTurn(0);
                        match.setCurrentTurnUserId(firstPlayerId(opponent));
                    }
                }
                break;
            }
            default:
                throw new BitpubException("Tipo di gioco non supportato per le azioni interattive", HttpStatus.BAD_REQUEST);
        }

        if (finished) {
            match.setStatus("COMPLETED");
            match.setEndTime(Instant.now());
            match.setCurrentTurnUserId(null);
            match.setTeamBased(isTeamBased(match));
        }

        teamRepository.saveAll(teams);
        Match saved = matchRepository.save(match);

        if (finished) {
            notifyStatisticsService(saved);
            publishGameState(saved, "FINISHED", "MATCH_END");
        } else {
            publishGameState(saved, "PLAYING", eventMessage);
        }

        return mapToDto(saved);
    }

    /**
     * Determines the winner team (highest score) and notifies the statistics-service
     * to update the leaderboard for both players.
     */
    /**
     * Team vincitore: a freccette vince chi ha il punteggio piu' basso (parte da 501 e scala a 0),
     * negli altri giochi vince chi ha il punteggio piu' alto.
     */
    private Team winnerTeam(Match match) {
        List<Team> teams = match.getTeams();
        if (teams == null || teams.size() < 2) return null;
        Comparator<Team> byScore = Comparator.comparingInt(Team::getScore);
        Team best = classify(match.getGameTypeId()) == GameKind.DARTS
                ? teams.stream().min(byScore).orElse(null)
                : teams.stream().max(byScore).orElse(null);
        int max = teams.stream().mapToInt(Team::getScore).max().orElse(0);
        int min = teams.stream().mapToInt(Team::getScore).min().orElse(0);
        return max == min ? null : best; // parita' di punteggio = pareggio, nessun vincitore
    }

    /**
     * Costruisce l'evento-risultato per la leaderboard, o null se il match non ha un vincitore
     * (meno di 2 team o pareggio di punteggio): un pareggio non muove la classifica.
     */
    private Map<String, Object> buildResultEvent(Match match) {
        if (match.getTeams() == null || match.getTeams().size() < 2) return null;
        Team winner = winnerTeam(match);
        if (winner == null) return null; // pareggio: nessun aggiornamento leaderboard
        Team loser = match.getTeams().stream()
                .filter(t -> !t.getId().equals(winner.getId()))
                .findFirst().orElse(null);

        Map<String, Object> resultEvent = new java.util.HashMap<>();
        resultEvent.put("gameTypeId", match.getGameTypeId());
        resultEvent.put("winnerName", winner.getName());
        resultEvent.put("loserName", loser != null ? loser.getName() : "Unknown");
        resultEvent.put("winnerScore", winner.getScore());
        resultEvent.put("loserScore", loser != null ? loser.getScore() : 0);
        resultEvent.put("winnerId", firstPlayerId(winner));
        resultEvent.put("loserId", loser != null ? firstPlayerId(loser) : null);
        resultEvent.put("matchId", match.getId());
        resultEvent.put("localeId", match.getLocaleId());
        resultEvent.put("teamBased", isTeamBased(match));
        return resultEvent;
    }

    private void notifyStatisticsService(Match match) {
        Map<String, Object> resultEvent = buildResultEvent(match);
        if (resultEvent == null) return;
        try {
            String body = objectMapper.writeValueAsString(resultEvent);
            RestClient.create(statisticsServiceUrl)
                    .post()
                    .uri("/api/v1/statistics/match-result")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notified statistics-service: winner={}, gameType={}",
                    resultEvent.get("winnerName"), match.getGameTypeId());
        } catch (Exception e) {
            log.error("Failed to notify statistics-service", e);
        }
    }

    /**
     * Backfill: ricostruisce la leaderboard dallo storico dei match gia' conclusi. Serve a
     * recuperare i match terminati mentre lo statistics-service era irraggiungibile (l'invio
     * live e' fire-and-forget). Invio in un'unica chiamata batch: lo statistics-service azzera
     * e ricostruisce in transazione, quindi l'operazione e' ripetibile senza doppi conteggi.
     */
    @Transactional(readOnly = true)
    public int backfillStatistics() {
        List<Map<String, Object>> events = matchRepository.findByStatus("COMPLETED").stream()
                .map(this::buildResultEvent)
                .filter(e -> e != null)
                .collect(Collectors.toList());
        try {
            String body = objectMapper.writeValueAsString(events);
            RestClient.create(statisticsServiceUrl)
                    .post()
                    .uri("/api/v1/statistics/leaderboard/rebuild")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Backfill verso statistics-service fallito", e);
            throw new BitpubException("Backfill verso statistics-service fallito", HttpStatus.BAD_GATEWAY);
        }
        log.info("Backfill leaderboard: {} match conclusi inviati", events.size());
        return events.size();
    }

    private void publishGameState(Match match, SensorEvent event) {
        String status = "IN_PROGRESS".equals(match.getStatus()) ? "PLAYING" : "FINISHED";
        publishGameState(match, status, event.getSensorType());
    }

    /**
     * Publishes lobby transitions (WAITING_FOR_PLAYERS / MATCH_START) triggered by the
     * PLAYER matchmaking flow, reusing the same GameStateDto/topic the Kiosk view already
     * consumes so the transition from waiting-room to live match is instantaneous.
     */
    private void publishLobbyState(Match match, String eventMessage) {
        String status = "WAITING_FOR_PLAYERS".equals(match.getStatus()) ? "WAITING" : "PLAYING";
        publishGameState(match, status, eventMessage);
    }

    private void publishGameState(Match match, String status, String eventMessage) {
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

        // Determine winner name on match end (game-aware: darts = lowest score)
        String winnerName = null;
        if ("FINISHED".equals(status)) {
            Team w = winnerTeam(match);
            winnerName = w != null ? w.getName() : null;
        }

        // Nomi dei giocatori con Piene/Spezzate (biliardo), risolti dagli id di team assegnati.
        String solidPlayerName = teamNameById(match, match.getSolidTeamId());
        String stripedPlayerName = teamNameById(match, match.getStripedTeamId());
        int throwsRemaining = Math.max(0, DARTS_THROWS_PER_TURN - match.getThrowsInTurn());

        GameStateDto stateDto = GameStateDto.builder()
                .matchId(match.getId())
                .gameTypeId(match.getGameTypeId())
                .status(status)
                .teamAName(teamAName)
                .teamBName(teamBName)
                .scoreTeamA(scoreA)
                .scoreTeamB(scoreB)
                .timeRemainingSeconds(0)
                .currentEventMessage(eventMessage)
                .winnerName(winnerName)
                .currentTurnUserId(match.getCurrentTurnUserId())
                .breakDone(match.isBreakDone())
                .solidPlayerName(solidPlayerName)
                .stripedPlayerName(stripedPlayerName)
                .throwsRemaining(throwsRemaining)
                .build();

        try {
            String statePayload = objectMapper.writeValueAsString(stateDto);
            String topicLocaleId = match.getLocaleId() != null ? match.getLocaleId() : "unknown";
            String topic = it.uniupo.pissir.bitpub.common.constants.MqttTopics.getGameStateTopic(topicLocaleId, match.getGameInstanceId());
            mqttOutboundChannel.send(MessageBuilder.withPayload(statePayload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .setHeader(MqttHeaders.RETAINED, true)
                    .build());
        } catch (Exception e) {
            log.error("Failed to publish GameStateDto", e);
        }
    }

    private String teamNameById(Match match, String teamId) {
        if (teamId == null || match.getTeams() == null) return null;
        return match.getTeams().stream()
                .filter(t -> teamId.equals(t.getId()))
                .map(Team::getName).findFirst().orElse(null);
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

        // Calcola il vincitore per esporlo nel DTO
        Team winnerTeamObj = "COMPLETED".equals(match.getStatus()) ? winnerTeam(match) : null;
        String winnerId = winnerTeamObj != null ? firstPlayerId(winnerTeamObj) : null;

        return MatchDto.builder()
                .id(match.getId())
                .gameInstanceId(match.getGameInstanceId())
                .localeId(match.getLocaleId())
                .gameTypeId(match.getGameTypeId())
                .status(match.getStatus())
                .teamBased(isTeamBased(match))
                .startTime(match.getStartTime())
                .endTime(match.getEndTime())
                .teams(teamDtos)
                .resultPayload(match.getResultPayload())
                .winnerId(winnerId)
                .currentTurnUserId(match.getCurrentTurnUserId())
                .breakDone(match.isBreakDone())
                .solidTeamId(match.getSolidTeamId())
                .stripedTeamId(match.getStripedTeamId())
                .build();
    }
}
