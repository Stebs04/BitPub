/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.statisticsservice.service.impl;

import it.uniupo.pissir.bitpub.statisticsservice.domain.AggregateStatistic;
import it.uniupo.pissir.bitpub.statisticsservice.domain.Leaderboard;
import it.uniupo.pissir.bitpub.statisticsservice.domain.MatchHistoryRecord;
import it.uniupo.pissir.bitpub.statisticsservice.repository.MatchHistoryRecordRepository;
import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.GameUsageDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.GlobalStatsDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.LeaderboardEntryDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.MatchResultEvent;
import it.uniupo.pissir.bitpub.statisticsservice.dto.StatisticUpdateRequest;
import it.uniupo.pissir.bitpub.statisticsservice.repository.AggregateStatisticRepository;
import it.uniupo.pissir.bitpub.statisticsservice.repository.LeaderboardRepository;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final AggregateStatisticRepository statisticRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final MatchHistoryRecordRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    @Qualifier("mqttOutboundChannel")
    private MessageChannel mqttOutboundChannel;

    @Value("${locale.service.url:http://localhost:8083}")
    private String localeServiceUrl;

    @Value("${user.service.url:http://localhost:8082}")
    private String userServiceUrl;

    @Value("${match.service.url:http://localhost:8085}")
    private String matchServiceUrl;

    @Value("${tournament.service.url:http://localhost:8086}")
    private String tournamentServiceUrl;

    /** Raccoglie i dati aggregati dai vari microservizi (Locali, Utenti, Match, Tornei) per costruire la dashboard globale. */
    @Override
    public GlobalStatsDto getGlobalOverview() {
        return GlobalStatsDto.builder()
                .totalLocales(fetchListSize(localeServiceUrl, "/api/v1/locales"))
                .totalUsers(fetchCount(userServiceUrl, "/api/v1/users/count"))
                .activeMatches(fetchListSize(matchServiceUrl, "/api/matches/active"))
                .activeTournaments(fetchListSize(tournamentServiceUrl, "/api/v1/tournaments/active"))
                .build();
    }

    private long fetchListSize(String baseUrl, String path) {
        try {
            List<?> response = RestClient.create(baseUrl).get().uri(path).retrieve().body(List.class);
            return response != null ? response.size() : 0;
        } catch (Exception e) {
            log.error("Failed to fetch {}{}", baseUrl, path, e);
            return 0;
        }
    }

    private long fetchCount(String baseUrl, String path) {
        try {
            Long response = RestClient.create(baseUrl).get().uri(path).retrieve().body(Long.class);
            return response != null ? response : 0;
        } catch (Exception e) {
            log.error("Failed to fetch {}{}", baseUrl, path, e);
            return 0;
        }
    }

    /** Risolve in maniera distribuita (chiamando il locale-service) l'ID del locale gestito dall'admin. */
    @Override
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

    @Override
    public List<AggregateStatisticDto> getStatisticsByEntity(String entityId, String entityType) {
        return statisticRepository.findByEntityIdAndEntityType(entityId, entityType).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AggregateStatisticDto updateStatistic(StatisticUpdateRequest request) {
        Optional<AggregateStatistic> optionalStat = statisticRepository
                .findByEntityIdAndEntityTypeAndMetricName(
                        request.getEntityId(), request.getEntityType(), request.getMetricName());

        AggregateStatistic stat;
        if (optionalStat.isPresent()) {
            stat = optionalStat.get();
            if (request.isAbsolute()) {
                stat.setMetricValue(request.getDeltaValue());
            } else {
                stat.setMetricValue(stat.getMetricValue() + request.getDeltaValue());
            }
        } else {
            stat = AggregateStatistic.builder()
                    .entityId(request.getEntityId())
                    .entityType(request.getEntityType())
                    .metricName(request.getMetricName())
                    .metricValue(request.getDeltaValue())
                    .build();
        }

        stat.setLastComputedAt(Instant.now());
        stat = statisticRepository.save(stat);

        return mapToDto(stat);
    }

    /**
     * Gestisce l'ingresso di un nuovo risultato aggiornando conseguentemente sia la classifica generale 
     * che le metriche aggregate personali dei giocatori coinvolti.
     */
    @Override
    @Transactional
    public void recordMatchResult(MatchResultEvent event) {
        log.info("Recording match result: winner={} ({}), loser={} ({}), gameType={}",
                event.getWinnerName(), event.getWinnerId(), event.getLoserName(), event.getLoserId(), event.getGameTypeId());

        // Controllo di idempotenza fondamentale: impedisce che una riconsegna MQTT (QoS 1) o un doppio invio falsifichi i contatori.
        if (event.getMatchId() != null && historyRepository.existsByMatchId(event.getMatchId())) {
            log.info("Match {} gia' registrato, ingest ignorato", event.getMatchId());
            return;
        }

        saveHistory(event);
        applyToLeaderboard(event);
        publishLeaderboardUpdate(event.getGameTypeId());

        // ── Aggiornamento Statistiche Personali ────────────────────────────────────
        // Viene eseguito solo qualora gli utenti possiedano un ID formale (iscritti ufficiali in piattaforma).
        boolean hasLoser = event.getLoserName() != null
                && !event.getLoserName().equalsIgnoreCase(event.getWinnerName());
        if (event.getWinnerId() != null) {
            incrementStat(event.getWinnerId(), "PLAYER", "WINS", 1);
            incrementStat(event.getWinnerId(), "PLAYER", "MATCHES_PLAYED", 1);
        }
        if (hasLoser && event.getLoserId() != null) {
            incrementStat(event.getLoserId(), "PLAYER", "LOSSES", 1);
            incrementStat(event.getLoserId(), "PLAYER", "MATCHES_PLAYED", 1);
        }
    }

    /** Persiste la traccia immutabile del match nello storico, per audit o ricalcoli futuri. */
    private void saveHistory(MatchResultEvent event) {
        historyRepository.save(MatchHistoryRecord.builder()
                .matchId(event.getMatchId())
                .gameTypeId(event.getGameTypeId())
                .winnerName(event.getWinnerName())
                .loserName(event.getLoserName())
                .winnerScore(event.getWinnerScore())
                .loserScore(event.getLoserScore())
                .teamBased(event.isTeamBased())
                .timestamp(Instant.now())
                .build());
    }

    /** Applica la logica di upsert sulle entry di classifica, gestendo le casistiche di vincitore e (opzionalmente) perdente. */
    private void applyToLeaderboard(MatchResultEvent event) {
        // I match collegati a un torneo non partecipano alla classifica pubblica standard per evitare alterazioni.
        if (event.getTournamentMatchId() != null) return;

        Leaderboard winner = leaderboardRepository
                .findByPlayerNameIgnoreCaseAndGameTypeId(event.getWinnerName(), event.getGameTypeId())
                .orElseGet(() -> Leaderboard.builder()
                        .playerName(event.getWinnerName())
                        .gameTypeId(event.getGameTypeId())
                        .build());
        winner.setWins(winner.getWins() + 1);
        winner.setTotalPoints(winner.getTotalPoints() + event.getWinnerScore());
        winner.setMatchesPlayed(winner.getMatchesPlayed() + 1);
        winner.setLastUpdated(Instant.now());
        winner.setLocaleId(event.getLocaleId());
        winner.setTeamBased(event.isTeamBased());
        leaderboardRepository.save(winner);

        boolean hasLoser = event.getLoserName() != null
                && !event.getLoserName().equalsIgnoreCase(event.getWinnerName());
        if (hasLoser) {
            Leaderboard loser = leaderboardRepository
                    .findByPlayerNameIgnoreCaseAndGameTypeId(event.getLoserName(), event.getGameTypeId())
                    .orElseGet(() -> Leaderboard.builder()
                            .playerName(event.getLoserName())
                            .gameTypeId(event.getGameTypeId())
                            .build());
            loser.setLosses(loser.getLosses() + 1);
            loser.setTotalPoints(loser.getTotalPoints() + event.getLoserScore());
            loser.setMatchesPlayed(loser.getMatchesPlayed() + 1);
            loser.setLastUpdated(Instant.now());
            loser.setLocaleId(event.getLocaleId());
            loser.setTeamBased(event.isTeamBased());
            leaderboardRepository.save(loser);
        }
    }

    /**
     * Propaga la nuova classifica via MQTT affinché i client connessi (es. interfaccia web) si aggiornino live.
     * Implementato con approccio fire-and-forget: un'eventuale anomalia non interromperà il flusso di persistenza.
     */
    private void publishLeaderboardUpdate(String gameTypeId) {
        if (gameTypeId == null) return; // Se il gioco è indefinito non vi è alcun aggiornamento da trasmettere
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "gameTypeId", gameTypeId,
                    "entries", getLeaderboard(gameTypeId)));
            mqttOutboundChannel.send(MessageBuilder.withPayload(json)
                    .setHeader(MqttHeaders.TOPIC, MqttTopics.getStatisticsUpdateTopic(gameTypeId))
                    .build());
        } catch (Exception e) {
            log.error("Publish leaderboard update per gameType {} fallito", gameTypeId, e);
        }
    }

    /**
     * Operazione di backfill: procede allo svuotamento massivo della classifica corrente e ad una sua completa ricostruzione
     * passandole in rassegna tutti i match storicizzati. Garantisce resilienza in caso di perdite dati.
     */
    @Override
    @Transactional
    public int rebuildLeaderboard(List<MatchResultEvent> events) {
        leaderboardRepository.deleteAll();
        if (events == null) return 0;
        events.forEach(this::applyToLeaderboard);
        // Si notificano i client UI per ogni tipologia di gioco impattata dal backfill, forzando la sincronizzazione.
        events.stream()
                .map(MatchResultEvent::getGameTypeId)
                .distinct()
                .forEach(this::publishLeaderboardUpdate);
        return events.size();
    }

    /** Incrementa il valore di una metrica aggregata o la inizializza in caso non esista. */
    private void incrementStat(String entityId, String entityType, String metricName, double delta) {
        AggregateStatistic stat = statisticRepository
                .findByEntityIdAndEntityTypeAndMetricName(entityId, entityType, metricName)
                .orElseGet(() -> AggregateStatistic.builder()
                        .entityId(entityId)
                        .entityType(entityType)
                        .metricName(metricName)
                        .metricValue(0)
                        .build());
        stat.setMetricValue(stat.getMetricValue() + delta);
        stat.setLastComputedAt(Instant.now());
        statisticRepository.save(stat);
    }

    @Override
    public List<LeaderboardEntryDto> getLeaderboard(String gameTypeId) {
        List<Leaderboard> entries = leaderboardRepository
                .findByGameTypeIdOrderByWinsDescTotalPointsDesc(gameTypeId);
        return entries.stream().map(this::mapToLeaderboardDto).collect(Collectors.toList());
    }

    @Override
    public List<String> getGameTypeIdsForPlayer(String playerName) {
        return leaderboardRepository.findDistinctGameTypeIdsByPlayerName(playerName);
    }

    @Override
    public List<LeaderboardEntryDto> getMyLeaderboardEntries(String playerName) {
        return leaderboardRepository.findByPlayerNameIgnoreCase(playerName).stream()
                .map(this::mapToLeaderboardDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaderboardEntryDto> getLeaderboardByLocale(String gameTypeId, String localeId) {
        List<Leaderboard> entries = leaderboardRepository
                .findByGameTypeIdAndLocaleIdOrderByWinsDescTotalPointsDesc(gameTypeId, localeId);
        return entries.stream().map(this::mapToLeaderboardDto).collect(Collectors.toList());
    }

    /**
     * Elabora e raggruppa le statistiche dei vari giochi in un locale, stabilendone la popolarità complessiva.
     * Il calcolo ordina i giochi per numero di partecipazioni riscontrate, individuando le attività di maggior successo.
     */
    @Override
    public List<GameUsageDto> getMostUsedGamesByLocale(String localeId) {
        Map<String, List<Leaderboard>> byGame = leaderboardRepository.findByLocaleId(localeId).stream()
                .collect(Collectors.groupingBy(Leaderboard::getGameTypeId));
        return byGame.entrySet().stream()
                .map(e -> GameUsageDto.builder()
                        .gameTypeId(e.getKey())
                        .matchesPlayed(e.getValue().stream().mapToInt(Leaderboard::getMatchesPlayed).sum())
                        .players(e.getValue().size())
                        .build())
                .sorted(Comparator.comparingInt(GameUsageDto::getMatchesPlayed).reversed())
                .collect(Collectors.toList());
    }

    // ── Sezione dedicata ai metodi di conversione (Mappers) ──

    private AggregateStatisticDto mapToDto(AggregateStatistic stat) {
        return AggregateStatisticDto.builder()
                .id(stat.getId())
                .entityId(stat.getEntityId())
                .entityType(stat.getEntityType())
                .metricName(stat.getMetricName())
                .metricValue(stat.getMetricValue())
                .lastComputedAt(stat.getLastComputedAt())
                .build();
    }

    private LeaderboardEntryDto mapToLeaderboardDto(Leaderboard entry) {
        return LeaderboardEntryDto.builder()
                .id(entry.getId())
                .playerName(entry.getPlayerName())
                .gameTypeId(entry.getGameTypeId())
                .localeId(entry.getLocaleId())
                .teamBased(entry.isTeamBased())
                .wins(entry.getWins())
                .losses(entry.getLosses())
                .totalPoints(entry.getTotalPoints())
                .matchesPlayed(entry.getMatchesPlayed())
                .lastUpdated(entry.getLastUpdated())
                .build();
    }
}
