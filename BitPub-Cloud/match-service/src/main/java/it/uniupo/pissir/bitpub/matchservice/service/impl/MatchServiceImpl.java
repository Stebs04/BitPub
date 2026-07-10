// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.matchservice.domain.Match;
import it.uniupo.pissir.bitpub.matchservice.domain.SensorEventLog;
import it.uniupo.pissir.bitpub.matchservice.domain.MatchParticipant;
import it.uniupo.pissir.bitpub.matchservice.dto.GameActionRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.JoinLobbyRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.ParticipantResponseDto;
import it.uniupo.pissir.bitpub.matchservice.repository.MatchRepository;
import it.uniupo.pissir.bitpub.matchservice.repository.SensorEventLogRepository;
import it.uniupo.pissir.bitpub.matchservice.repository.MatchParticipantRepository;
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
    private final MatchParticipantRepository participantRepository;
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
     * Verifica l'appartenenza di un utente a un match di torneo.
     * L'accesso è ristretto esclusivamente ai due giocatori associati allo scontro nel tabellone.
     * Viene effettuata una verifica sul servizio tornei, con rifiuto in caso di esito negativo o errore.
     */
    private void assertTournamentPairing(String tournamentMatchId, String playerId) {
        if (tournamentMatchId == null) {
            return; // In caso di partita libera, non sussistono vincoli di abbinamento
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
     * Recupera l'identificativo del locale di appartenenza per l'istanza di gioco, 
     * consultando il servizio locale (locale-service). Necessario per circoscrivere l'accesso
     * degli amministratori (LOCALE_ADMIN) esclusivamente alle partite della loro sede.
     */
    private String resolveLocaleId(String gameInstanceId) {
        Map info = fetchGameInstanceInfo(gameInstanceId);
        return info != null && info.containsKey("localeId") ? info.get("localeId").toString() : null;
    }

    /**
     * Recupera le informazioni complete dell'istanza di gioco (localeId, gameTypeId, stato attivo)
     * dal servizio dedicato. Viene impiegato nella procedura di matchmaking per accertarsi che la postazione
     * sia operativa e per popolare i dati della lobby.
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
     * Implementa i vincoli di sicurezza: un amministratore di locale (LOCALE_ADMIN) può accedere
     * solo ed esclusivamente alle partite relative al proprio locale. Gli amministratori globali (PLATFORM_ADMIN)
     * e i giocatori mantengono l'accesso in lettura incondizionato.
     * Privilegia il parametro estratto dal token JWT inoltrato dal gateway e, in assenza, 
     * invoca il servizio locale per risolverlo tramite l'ID utente.
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

    /** Restituisce l'identificativo del locale associato all'amministratore, se presente. */
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
        // Validazione: controlla se per l'istanza specificata è già presente una partita in corso
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

        List<String> names = request.getPlayerNames() != null ? request.getPlayerNames() : List.of();
        List<MatchParticipant> teams = names.stream().map(name -> {
            List<String> playerIds = new ArrayList<>();
            String userId = ensureUser(name);
            if (userId != null) {
                playerIds.add(userId);
            }
            return MatchParticipant.builder()
                .name(name)
                .playerIds(playerIds)
                .match(savedMatch)
                .score(0)
                .build();
        }).collect(Collectors.toList());

        participantRepository.saveAll(teams);
        savedMatch.setTeams(teams);

        publishMatchSync(savedMatch);
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

        // Il vincitore viene calcolato automaticamente tramite la funzione winnerTeam() 
        // valutando il punteggio migliore a seconda della natura del gioco.
        notifyStatisticsService(saved); // Propaga i dati per le statistiche e per il torneo tramite topic dedicato
        publishGameState(saved, "FINISHED", "MATCH_END");

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public MatchDto applyFinalResult(String matchId, Map<String, Integer> scoresByTeamName) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));

        // Controllo di idempotenza: i nodi Edge inoltrano aggiornamenti best-effort ed eventuali duplicati 
        // devono essere ignorati se la partita risulta già finalizzata.
        if ("COMPLETED".equals(match.getStatus())) {
            return mapToDto(match);
        }

        if (match.getTeams() != null && scoresByTeamName != null) {
            for (MatchParticipant t : match.getTeams()) {
                Integer s = scoresByTeamName.get(t.getName());
                if (s != null) {
                    t.setScore(s);
                }
            }
        }

        match.setStatus("COMPLETED");
        match.setEndTime(Instant.now());
        match.setCurrentTurnUserId(null);
        match.setTeamBased(isTeamBased(match));
        Match saved = matchRepository.save(match);

        notifyStatisticsService(saved); // Propaga asincronamente i risultati per l'aggiornamento statistiche e tornei
        // Nessuna pubblicazione di stato: il termine della partita (FINISHED) è gestito in modo autoritativo dall'Edge.

        return mapToDto(saved);
    }

    /**
     * Procedura di matchmaking lato giocatore: se è già presente una lobby in attesa 
     * (WAITING_FOR_PLAYERS) per questa postazione di gioco, vi inserisce il chiamante come 
     * secondo partecipante e ne aggiorna lo stato a IN_PROGRESS ("STARTED"). Contestualmente,
     * notifica l'avanzamento tramite MQTT affinché entrambi i client elaborino la transizione 
     * simultaneamente. In assenza di lobby, provvede all'inizializzazione di una nuova sessione.
     */
    @Override
    @Transactional
    public MatchDto joinLobby(JoinLobbyRequestDto request, String playerId) {
        String gameInstanceId = request.getGameInstanceId();
        String username = request.getUsername();

        // Gestione Torneo: si assicura che il richiedente corrisponda a uno dei due giocatori attesi dallo scontro.
        assertTournamentPairing(request.getTournamentMatchId(), playerId);

        Optional<Match> waiting = matchRepository.findFirstByGameInstanceIdAndStatusOrderByStartTimeDesc(gameInstanceId, "WAITING_FOR_PLAYERS");

        if (waiting.isPresent()) {
            Match match = waiting.get();

            // Meccanismo idempotente per la riconnessione: evita che la duplicazione di richieste 
            // generi molteplici inserimenti dello stesso giocatore nella medesima squadra.
            boolean alreadyIn = match.getTeams() != null && match.getTeams().stream()
                    .anyMatch(t -> t.getPlayerIds() != null && t.getPlayerIds().contains(playerId));
            if (alreadyIn) {
                return mapToDto(match);
            }

            MatchParticipant secondTeam = MatchParticipant.builder()
                    .name(username)
                    .playerIds(new ArrayList<>(List.of(playerId)))
                    .score(0)
                    .match(match)
                    .build();
            participantRepository.save(secondTeam);
            match.getTeams().add(secondTeam);

            match.setStatus("IN_PROGRESS");
            match.setStartTime(Instant.now());
            // Inizializza il turno assegnandolo al primo giocatore (squadra A, creatore della lobby).
            // A partire da questa operazione iniziale, l'Edge si farà carico di aggiornarlo ad ogni successiva interazione.
            match.setCurrentTurnUserId(firstPlayerId(match.getTeams().get(0)));
            Match saved = matchRepository.save(match);

            publishLobbyState(saved, "MATCH_START");
            publishMatchSync(saved);
            log.info("Lobby {} STARTED: {} joined gameInstanceId {}", saved.getId(), username, gameInstanceId);
            return mapToDto(saved);
        }

        // Qualora non ci siano lobby in attesa: convalida lo stato operativo della postazione interrogando il relativo servizio.
        Map info = fetchGameInstanceInfo(gameInstanceId);
        boolean active = info != null && Boolean.TRUE.equals(info.get("active"));
        if (info == null || !active) {
            throw new BitpubException("Il gioco selezionato non e' attivo in questo momento", HttpStatus.CONFLICT);
        }
        String localeId = info.get("localeId") != null ? info.get("localeId").toString() : null;
        // Poiché il servizio locale può fornire alternativamente l'UUID o il nome del catalogo, 
        // è necessario normalizzare il dato sul formato atteso dal frontend,
        // con un fallback sul parametro localInstanceId (es. "calciobalilla-1").
        String rawGameType = info.get("gameTypeId") != null ? info.get("gameTypeId").toString() : null;
        String localInstanceId = info.get("localInstanceId") != null ? info.get("localInstanceId").toString() : null;
        String gameTypeId = rawGameType != null ? rawGameType : localInstanceId;

        Match match = Match.builder()
                .gameInstanceId(gameInstanceId)
                .localeId(localeId)
                .gameTypeId(gameTypeId)
                .status("WAITING_FOR_PLAYERS")
                .tournamentMatchId(request.getTournamentMatchId()) // Un valore null denota una partita libera (non classificata)
                .teams(new ArrayList<>())
                .build();
        Match saved = matchRepository.save(match);

        MatchParticipant firstTeam = MatchParticipant.builder()
                .name(username)
                .playerIds(new ArrayList<>(List.of(playerId)))
                .score(0)
                .match(saved)
                .build();
        participantRepository.save(firstTeam);
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
     * Storico delle partite del giocatore, destinato alle dashboard e alle visualizzazioni statistiche.
     * Vista la mancanza di un selettore specifico nel repository per la collezione integrata (element-collection)
     * contenente i playerIds, il filtraggio viene applicato in memoria.
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

        // Validazione della chiave di idempotenza per garantire un solo processo per evento
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

            // Estrapolazione delle etichette per i giocatori dal payload MQTT inviato dal simulatore
            String teamAName = "RED";
            String teamBName = "BLUE";
            if (event.getPayload() != null) {
                if (event.getPayload().containsKey("teamAName")) teamAName = event.getPayload().get("teamAName").toString();
                if (event.getPayload().containsKey("teamBName")) teamBName = event.getPayload().get("teamBName").toString();
            }

            String userAId = ensureUser(teamAName);
            String userBId = ensureUser(teamBName);

            match.getTeams().add(MatchParticipant.builder()
                .name(teamAName)
                .playerIds(userAId != null ? new ArrayList<>(List.of(userAId)) : new ArrayList<>())
                .score(0)
                .match(match)
                .build());
            match.getTeams().add(MatchParticipant.builder()
                .name(teamBName)
                .playerIds(userBId != null ? new ArrayList<>(List.of(userBId)) : new ArrayList<>())
                .score(0)
                .match(match)
                .build());

            participantRepository.saveAll(match.getTeams());
            match.setCurrentTurnUserId(firstPlayerId(match.getTeams().get(0)));
            match = matchRepository.save(match);
            activeMatchOpt = Optional.of(match);
            publishMatchSync(match);
            log.info("Auto-created match {} for gameInstanceId {}", match.getId(), match.getGameInstanceId());
        }

        if (activeMatchOpt.isPresent()) {
            match = activeMatchOpt.get();
            String type = event.getSensorType();

            // Funzione di sola persistenza: la logica esecutiva (turni, punteggio, broadcasting) è delegata 
            // al nodo Edge locale. La definizione del punteggio e la corretta terminazione del processo arrivano 
            // tramite l'endpoint dedicato ai risultati (applyFinalResult). In questa sezione si intercetta soltanto la
            // chiusura formale (MATCH_END) indotta dal simulatore o dall'autoplay, proteggendola con uno stato COMPLETED.
            if ("MATCH_END".equals(type) && !"COMPLETED".equals(match.getStatus())) {
                match.setStatus("COMPLETED");
                match.setEndTime(Instant.now());
                match.setTeamBased(isTeamBased(match));
                matchRepository.save(match);
                notifyStatisticsService(match); // Pubblicazione per l'aggiornamento simultaneo di statistiche e tornei
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
        // Nessuna trasmissione dello stato: si affida all'Edge il compito di annunciare l'evento sul topic autoritativo match-state.
    }

    // ── Logica di esecuzione delle azioni di gioco (RNG delegato al GenericSimulator) ───

    /** Partita a squadre se almeno un team ha piu' di un giocatore; altrimenti individuale. */
    private boolean isTeamBased(Match match) {
        return match.getTeams() != null && match.getTeams().stream()
                .anyMatch(t -> t.getPlayerIds() != null && t.getPlayerIds().size() > 1);
    }

    private String firstPlayerId(MatchParticipant team) {
        return team != null && team.getPlayerIds() != null && !team.getPlayerIds().isEmpty()
                ? team.getPlayerIds().get(0) : null;
    }

    /**
     * Interazione manuale del giocatore. Il match-service declina l'esecuzione attiva: individua 
     * esclusivamente la squadra di appartenenza e innesca la comunicazione col GenericSimulator 
     * tramite MQTT (sul topic bitpub/simulators/{gameInstanceId}/action). Il simulatore produce
     * la valutazione RNG e trasmette nuovamente l'evento, innescando l'aggiornamento del punteggio.
     * La funzione si chiude riportando lo stato asincrono corrente.
     */
    @Override
    @Transactional(readOnly = true)
    public MatchDto processGameAction(String matchId, String playerId, GameActionRequestDto action) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", "id", matchId));

        if (!"IN_PROGRESS".equals(match.getStatus())) {
            throw new BitpubException("La partita non e' in corso", HttpStatus.CONFLICT);
        }

        List<MatchParticipant> teams = match.getTeams();
        if (teams == null || teams.isEmpty()) {
            throw new BitpubException("Partita senza giocatori", HttpStatus.CONFLICT);
        }

        // L'autorizzazione per i turni è demandata all'Edge. Nel contesto Cloud si accerta solamente
        // a chi associare l'azione e l'etichetta del pacchetto generato per il simulatore;
        // qualora l'identità non sia deducibile, si adotta un approccio fall-back verso la prima squadra disponibile.
        MatchParticipant current = teams.stream()
                .filter(t -> t.getPlayerIds() != null && t.getPlayerIds().contains(playerId))
                .findFirst()
                .orElse(teams.get(0));

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
        request.put("eventId", action.getEventId()); // Estende l'identificativo idempotente al nuovo evento simulato

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
     * Individua il partecipante vincitore sulla base della natura del gioco. Nel nostro paradigma data-driven,
     * ogni disciplina accumula punti per raggiungere il target configurato; si premia dunque il valore massimo.
     * Il pareggio numerico implica la mancanza di un vincitore assoluto.
     */
    private MatchParticipant winnerTeam(Match match) {
        List<MatchParticipant> teams = match.getTeams();
        if (teams == null || teams.size() < 2) return null;
        MatchParticipant best = teams.stream().max(Comparator.comparingInt(MatchParticipant::getScore)).orElse(null);
        int max = teams.stream().mapToInt(MatchParticipant::getScore).max().orElse(0);
        int min = teams.stream().mapToInt(MatchParticipant::getScore).min().orElse(0);
        return max == min ? null : best;
    }

    /**
     * Crea un oggetto evento per l'aggiornamento della classifica. Restituisce nullo se la 
     * partita si conclude in parità (e.g. meno di due team o punteggi speculari), in quanto 
     * un pareggio non innesca spostamenti in leaderboard.
     */
    private Map<String, Object> buildResultEvent(Match match) {
        if (match.getTeams() == null || match.getTeams().size() < 2) return null;
        MatchParticipant winner = winnerTeam(match);
        if (winner == null) return null; // Pareggio: nessun progresso sulla classifica
        MatchParticipant loser = match.getTeams().stream()
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
        // Include l'identificativo del team nello scontro per supportare il tournament-service 
        // nell'allocazione dei goal attribuiti, specialmente per team multi-giocatore.
        resultEvent.put("winnerTeamId", winner.getId());
        resultEvent.put("loserTeamId", loser != null ? loser.getId() : null);
        resultEvent.put("matchId", match.getId());
        resultEvent.put("localeId", match.getLocaleId());
        resultEvent.put("teamBased", isTeamBased(match));
        // Riferimento opzionale del bracket match: essenziale per convogliare i dati al tournament-service
        // e separare il progresso torneistico dalle statistiche di utilizzo globali.
        resultEvent.put("tournamentMatchId", match.getTournamentMatchId());
        return resultEvent;
    }

    /**
     * Inoltra sul topic MQTT asincrono l'esito della partita (QoS1). Questo approccio rimpiazza la 
     * tradizionale chiamata REST, assicurando che in caso d'irraggiungibilità dello statistics-service 
     * il broker accodi i risultati per la successiva elaborazione. Questa garanzia preserva lo stato 
     * del database eliminando la necessità di operazioni costanti di riallineamento manuale.
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
     * Meccanismo di compensazione retroattiva (backfill) per i match archiviati. Interviene come garanzia di
     * ripristino per quelle rare eccezioni di smarrimento o interruzione dei task.
     * Operazione aggregata a livello transazionale sullo statistics-service.
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

    /**
     * Propaga in broadcast ai canali asincroni il cambiamento di stato delle lobby.
     * L'interfaccia client Kiosk attinge da questi per assicurare transizioni visive 
     * immediate al subentro dei giocatori.
     */
    private void publishLobbyState(Match match, String eventMessage) {
        String status = "WAITING_FOR_PLAYERS".equals(match.getStatus()) ? "WAITING" : "PLAYING";
        publishGameState(match, status, eventMessage);
    }

    /**
     * Notifica in maniera estesa i nodi Edge via MQTT qualora un match avvii il suo iter, 
     * permettendo alle periferiche di costruire un LocalMatchState reattivo partendo da tale flusso push
     * senza dover appesantire le interfacce REST.
     * In assenza di clausole di ritenzione estreme, accoglie il vincolo architetturale in base al quale
     * una macchina operativa conserverà lo stato nella memoria RAM.
     */
    private void publishMatchSync(Match match) {
        try {
            String payload = objectMapper.writeValueAsString(mapToDto(match));
            String topic = it.uniupo.pissir.bitpub.common.constants.MqttTopics.getEdgeMatchSyncTopic(match.getId());
            mqttOutboundChannel.send(MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, topic)
                    .build());
            log.info("Published match sync for {} to Edge topic {}", match.getId(), topic);
        } catch (Exception e) {
            log.error("Failed to publish match sync for {}", match.getId(), e);
        }
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
            MatchParticipant w = winnerTeam(match);
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
                .currentTurnUserId("FINISHED".equals(status) ? null : match.getCurrentTurnUserId())
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
        List<ParticipantResponseDto> teamDtos = match.getTeams() == null ? List.of() : match.getTeams().stream()
                .map(t -> ParticipantResponseDto.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .playerIds(t.getPlayerIds() != null ? new java.util.ArrayList<>(t.getPlayerIds()) : new java.util.ArrayList<>())
                        .score(t.getScore())
                        .build())
                .collect(Collectors.toList());

        // Esecuzione calcolo vincitore e integrazione delle rispettive proprietà per il DTO finale
        MatchParticipant winnerTeamObj = "COMPLETED".equals(match.getStatus()) ? winnerTeam(match) : null;
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
