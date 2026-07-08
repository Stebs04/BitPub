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

    @Value("${tournament.service.url:http://localhost:8086}")
    private String tournamentServiceUrl;

    /**
     * Partita di torneo: solo i due giocatori abbinati nello scontro del tabellone possono
     * connettersi. Chiede a tournament-service; fail-closed (403) se la verifica non riesce.
     */
    private void assertTournamentPairing(String tournamentMatchId, String playerId) {
        if (tournamentMatchId == null) {
            return; // partita libera, nessun abbinamento da rispettare
        }
        Boolean allowed;
        try {
            allowed = RestClient.create(tournamentServiceUrl)
                    .get()
                    .uri("/api/v1/tournaments/matches/{id}/authorize?playerId={pid}", tournamentMatchId, playerId)
                    .retrieve()
                    .body(Boolean.class);
        } catch (Exception e) {
            log.error("Tournament pairing check failed for match {} player {}", tournamentMatchId, playerId, e);
            throw new BitpubException("Impossibile verificare l'abbinamento del torneo", HttpStatus.BAD_GATEWAY);
        }
        if (!Boolean.TRUE.equals(allowed)) {
            throw new BitpubException("Non sei abbinato a questa partita del torneo", HttpStatus.FORBIDDEN);
        }
    }

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
                .gameTypeId(request.getGameTypeId() != null ? request.getGameTypeId() : request.getGameInstanceId())
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
        notifyTournamentResult(saved);
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

        // Torneo: verifica che il chiamante sia uno dei due giocatori abbinati nel tabellone.
        assertTournamentPairing(request.getTournamentMatchId(), playerId);

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
        // Il locale-service passa a volte l'UUID del catalogo, a volte il nome: normalizza al token
        // stabile del frontend, ripiegando sul localInstanceId (es. "calciobalilla-1") se serve.
        String rawGameType = info.get("gameTypeId") != null ? info.get("gameTypeId").toString() : null;
        String localInstanceId = info.get("localInstanceId") != null ? info.get("localInstanceId").toString() : null;
        String gameTypeId = rawGameType != null ? rawGameType : localInstanceId;

        Match match = Match.builder()
                .gameInstanceId(gameInstanceId)
                .localeId(localeId)
                .gameTypeId(gameTypeId)
                .status("WAITING_FOR_PLAYERS")
                .tournamentMatchId(request.getTournamentMatchId()) // null = partita libera
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
            Map giInfo = fetchGameInstanceInfo(event.getGameInstanceId());
            String autoLocaleId = giInfo != null && giInfo.get("localeId") != null ? giInfo.get("localeId").toString() : null;
            String autoGameTypeId = giInfo != null && giInfo.get("gameTypeId") != null
                    ? giInfo.get("gameTypeId").toString() : event.getGameInstanceId();
            match = Match.builder()
                .gameInstanceId(event.getGameInstanceId())
                .localeId(autoLocaleId)
                .gameTypeId(autoGameTypeId)
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

            Map<String, Object> payload = event.getPayload();
            String type = event.getSensorType();

            if ("MATCH_END".equals(type)) {
                match.setStatus("COMPLETED");
                match.setEndTime(Instant.now());
                match.setTeamBased(isTeamBased(match));
                matchRepository.save(match);
                notifyStatisticsService(match);
                notifyTournamentResult(match);
            } else if (!"MATCH_START".equals(type)) {
                // Sensore di gioco generico e data-driven: il simulatore ha gia' deciso l'esito e
                // stampato scoreIncrement (0 = MISS) e winScoreTarget nel payload. Qui accreditiamo
                // i punti al team indicato e chiudiamo la partita al raggiungimento del traguardo.
                int scoreIncrement = payload != null && payload.get("scoreIncrement") != null
                        ? Integer.parseInt(payload.get("scoreIncrement").toString()) : 0;
                if (scoreIncrement != 0) {
                    Team scored = resolveTeam(match, payload);
                    if (scored != null) {
                        scored.setScore(scored.getScore() + scoreIncrement);
                        matchRepository.save(match);

                        int winTarget = payload != null && payload.get("winScoreTarget") != null
                                ? Integer.parseInt(payload.get("winScoreTarget").toString()) : Integer.MAX_VALUE;
                        if (scored.getScore() >= winTarget) {
                            match.setStatus("COMPLETED");
                            match.setEndTime(Instant.now());
                            match.setTeamBased(isTeamBased(match));
                            matchRepository.save(match);
                            notifyStatisticsService(match);
                            notifyTournamentResult(match);
                        }
                    }
                }
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

    // ── Azioni di gioco (RNG delegato al GenericSimulator) ───────────────────────

    /** Partita a squadre se almeno un team ha piu' di un giocatore; altrimenti individuale. */
    private boolean isTeamBased(Match match) {
        return match.getTeams() != null && match.getTeams().stream()
                .anyMatch(t -> t.getPlayerIds() != null && t.getPlayerIds().size() > 1);
    }

    private String firstPlayerId(Team team) {
        return team != null && team.getPlayerIds() != null && !team.getPlayerIds().isEmpty()
                ? team.getPlayerIds().get(0) : null;
    }

    /** Team accreditato da un sensor event: quello nominato nel payload ("team"), o il primo. */
    private Team resolveTeam(Match match, Map<String, Object> payload) {
        if (match.getTeams() == null || match.getTeams().isEmpty()) return null;
        if (payload != null && payload.get("team") != null) {
            String name = payload.get("team").toString();
            return match.getTeams().stream()
                    .filter(t -> t.getName() != null && t.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(match.getTeams().get(0));
        }
        return match.getTeams().get(0);
    }

    /**
     * Azione interattiva del giocatore. Il match-service NON calcola piu' l'esito: individua la
     * squadra di chi agisce e inoltra la richiesta al GenericSimulator via MQTT
     * (bitpub/simulators/{gameInstanceId}/action). Il simulatore tira l'RNG sulla successProbability
     * del sensore e ripubblica il sensor event risultante (con scoreIncrement) o un MISS, che rientra
     * da processSensorEvent e aggiorna il punteggio. Ritorna lo stato corrente (aggiornato in async).
     */
    @Override
    @Transactional(readOnly = true)
    public MatchDto processGameAction(String matchId, String playerId, GameActionRequestDto action) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));

        if (!"IN_PROGRESS".equals(match.getStatus())) {
            throw new BitpubException("La partita non e' in corso", HttpStatus.CONFLICT);
        }

        List<Team> teams = match.getTeams();
        if (teams == null || teams.isEmpty()) {
            throw new BitpubException("Partita senza giocatori", HttpStatus.CONFLICT);
        }

        Team current = teams.stream()
                .filter(t -> t.getPlayerIds() != null && t.getPlayerIds().contains(playerId))
                .findFirst()
                .orElseThrow(() -> new BitpubException("Giocatore non nel match", HttpStatus.FORBIDDEN));

        String sensorType = action.getSensorType();
        if (sensorType == null || sensorType.isBlank()) {
            throw new BitpubException("sensorType mancante nell'azione", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("gameInstanceId", match.getGameInstanceId());
        request.put("localeId", match.getLocaleId());
        request.put("gameTypeId", match.getGameTypeId());
        request.put("matchId", match.getId());
        request.put("sensorType", sensorType);
        request.put("team", current.getName());
        request.put("eventId", action.getEventId()); // idempotenza propagata al sensor event

        try {
            String body = objectMapper.writeValueAsString(request);
            mqttOutboundChannel.send(MessageBuilder.withPayload(body)
                    .setHeader(MqttHeaders.TOPIC,
                            it.uniupo.pissir.bitpub.common.constants.MqttTopics.getSimulatorActionTopic(match.getGameInstanceId()))
                    .build());
            log.info("Routed action {} to simulator for match {} (team {})", sensorType, matchId, current.getName());
        } catch (Exception e) {
            log.error("Failed to route game action to simulator", e);
            throw new BitpubException("Impossibile inoltrare l'azione al simulatore", HttpStatus.BAD_GATEWAY);
        }

        return mapToDto(match);
    }

    /**
     * Determines the winner team (highest score) and notifies the statistics-service
     * to update the leaderboard for both players.
     */
    /**
     * Team vincitore: nel modello data-driven ogni gioco somma punti verso winScoreTarget, quindi
     * vince sempre chi ha il punteggio piu' alto. Parita' di punteggio = pareggio (nessun vincitore).
     */
    private Team winnerTeam(Match match) {
        List<Team> teams = match.getTeams();
        if (teams == null || teams.size() < 2) return null;
        Team best = teams.stream().max(Comparator.comparingInt(Team::getScore)).orElse(null);
        int max = teams.stream().mapToInt(Team::getScore).max().orElse(0);
        int min = teams.stream().mapToInt(Team::getScore).min().orElse(0);
        return max == min ? null : best;
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
        // Bracket slot (null se non e' una partita di torneo): il tournament-service lo usa per
        // attribuire i gol al torneo corretto, isolandoli dalla leaderboard globale.
        resultEvent.put("tournamentMatchId", match.getTournamentMatchId());
        return resultEvent;
    }

    /**
     * Partita di torneo conclusa: riporta vincitore e punteggio al tournament-service, che registra
     * l'esito nel tabellone e fa avanzare il vincitore al match successivo. Fire-and-forget: un
     * errore qui non deve far fallire la chiusura della partita (le stats restano recuperabili
     * dal tournamentMatchId salvato sul match). Salta i pareggi (nessun avanzamento possibile).
     */
    private void notifyTournamentResult(Match match) {
        if (match.getTournamentMatchId() == null) return;
        Team winner = winnerTeam(match);
        String winnerId = firstPlayerId(winner);
        if (winnerId == null) {
            log.warn("Match {} di torneo senza vincitore (pareggio): esito non riportato", match.getId());
            return;
        }
        String stats = match.getTeams().stream()
                .map(t -> t.getName() + " " + t.getScore())
                .collect(Collectors.joining(" - "));
        try {
            RestClient.create(tournamentServiceUrl)
                    .post()
                    .uri("/api/v1/tournaments/matches/{id}/result?winnerId={w}&stats={s}",
                            match.getTournamentMatchId(), winnerId, stats)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Esito torneo riportato: bracketMatch={}, winnerId={}", match.getTournamentMatchId(), winnerId);
        } catch (Exception e) {
            log.error("Riporto esito al tournament-service fallito per bracketMatch {}", match.getTournamentMatchId(), e);
        }
    }

    /**
     * Pubblica il risultato del match concluso su MQTT (bitpub/cloud/matches/result); lo statistics-service
     * lo consuma in modo asincrono e durevole (QoS1). Sostituisce la vecchia POST REST sincrona: se lo
     * statistics-service e' giu', il broker accoda il risultato e lo riconsegna al riavvio, quindi le
     * partite concluse finiscono comunque in classifica senza dover ricorrere al backfill manuale.
     * L'ingest e' idempotente sul matchId, quindi una riconsegna QoS1 non raddoppia i conteggi.
     */
    private void notifyStatisticsService(Match match) {
        Map<String, Object> resultEvent = buildResultEvent(match);
        if (resultEvent == null) return;
        try {
            String body = objectMapper.writeValueAsString(resultEvent);
            mqttOutboundChannel.send(MessageBuilder.withPayload(body)
                    .setHeader(MqttHeaders.TOPIC, it.uniupo.pissir.bitpub.common.constants.MqttTopics.CLOUD_MATCH_RESULT_TOPIC)
                    .build());
            log.info("Published match result to MQTT: winner={}, gameType={}",
                    resultEvent.get("winnerName"), match.getGameTypeId());
        } catch (Exception e) {
            log.error("Failed to publish match result to MQTT", e);
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

        String winnerName = null;
        if ("FINISHED".equals(status)) {
            Team w = winnerTeam(match);
            winnerName = w != null ? w.getName() : null;
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
                .currentEventMessage(eventMessage)
                .winnerName(winnerName)
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
