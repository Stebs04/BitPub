package it.uniupo.pissir.bitpub.statisticsservice.service.impl;

import it.uniupo.pissir.bitpub.statisticsservice.domain.AggregateStatistic;
import it.uniupo.pissir.bitpub.statisticsservice.domain.Leaderboard;
import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.GlobalStatsDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.LeaderboardEntryDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.MatchResultEvent;
import it.uniupo.pissir.bitpub.statisticsservice.dto.StatisticUpdateRequest;
import it.uniupo.pissir.bitpub.statisticsservice.repository.AggregateStatisticRepository;
import it.uniupo.pissir.bitpub.statisticsservice.repository.LeaderboardRepository;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
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

    @Value("${locale.service.url:http://localhost:8083}")
    private String localeServiceUrl;

    @Value("${user.service.url:http://localhost:8082}")
    private String userServiceUrl;

    @Value("${match.service.url:http://localhost:8085}")
    private String matchServiceUrl;

    @Value("${tournament.service.url:http://localhost:8086}")
    private String tournamentServiceUrl;

    /** Aggregates platform-wide monitoring data for the PLATFORM_ADMIN dashboard. */
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

    /** Returns the localeId of the locale owned by the given adminId, or null if none. */
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
     * Records the result of a completed match and upserts leaderboard entries for winner and loser.
     * Also updates per-userId AggregateStatistic (WINS, LOSSES, MATCHES_PLAYED) when userId is known.
     */
    @Override
    @Transactional
    public void recordMatchResult(MatchResultEvent event) {
        log.info("Recording match result: winner={} ({}), loser={} ({}), gameType={}",
                event.getWinnerName(), event.getWinnerId(), event.getLoserName(), event.getLoserId(), event.getGameTypeId());

        // ── Leaderboard (by name) ──────────────────────────────────────────────
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
            leaderboardRepository.save(loser);
        }

        // ── AggregateStatistic (by userId) ────────────────────────────────────
        // ponytail: aggiorna solo se winnerId/loserId sono valorizzati (partite dal matchmaking player)
        if (event.getWinnerId() != null) {
            incrementStat(event.getWinnerId(), "PLAYER", "WINS", 1);
            incrementStat(event.getWinnerId(), "PLAYER", "MATCHES_PLAYED", 1);
        }
        if (hasLoser && event.getLoserId() != null) {
            incrementStat(event.getLoserId(), "PLAYER", "LOSSES", 1);
            incrementStat(event.getLoserId(), "PLAYER", "MATCHES_PLAYED", 1);
        }
    }

    /** Increment-or-create an AggregateStatistic by 1. */
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
    public List<LeaderboardEntryDto> getLeaderboardByLocale(String gameTypeId, String localeId) {
        List<Leaderboard> entries = leaderboardRepository
                .findByGameTypeIdAndLocaleIdOrderByWinsDescTotalPointsDesc(gameTypeId, localeId);
        return entries.stream().map(this::mapToLeaderboardDto).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------

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
                .wins(entry.getWins())
                .losses(entry.getLosses())
                .totalPoints(entry.getTotalPoints())
                .matchesPlayed(entry.getMatchesPlayed())
                .lastUpdated(entry.getLastUpdated())
                .build();
    }
}
