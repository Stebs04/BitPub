package com.bitpub.stats.service;

import com.bitpub.stats.dto.LeaderboardEntryDto;
import com.bitpub.stats.dto.PagedResponseDto;
import com.bitpub.stats.dto.RecordMatchRequest;

import com.bitpub.stats.model.MatchResult;
import com.bitpub.stats.model.PlayerStats;
import com.bitpub.stats.repository.MatchResultRepository;
import com.bitpub.stats.repository.PlayerStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayerStatsRepository playerStatsRepository;
    private final MatchResultRepository matchResultRepository;

    @Transactional
    @CacheEvict(value = "leaderboards", allEntries = true)
    public MatchResult recordMatch(RecordMatchRequest request) {
        if (matchResultRepository.existsByMatchSessionId(request.getMatchSessionId())) {
            log.warn("Match session {} already recorded, skipping", request.getMatchSessionId());
            return matchResultRepository
                    .findByWinnerUserIdOrLoserUserIdOrderByPlayedAtDesc(request.getWinnerUserId(), request.getLoserUserId())
                    .stream().findFirst().orElseThrow();
        }

        MatchResult result = MatchResult.builder()
                .matchSessionId(request.getMatchSessionId())
                .gameId(request.getGameId())
                .winnerUserId(request.getWinnerUserId())
                .loserUserId(request.getLoserUserId())
                .winnerScore(request.getWinnerScore())
                .loserScore(request.getLoserScore())
                .build();
        matchResultRepository.save(result);

        updatePlayerStats(request.getWinnerUserId(), request.getWinnerUsername(),
                request.getGameId(), true, request.getWinnerScore());

        updatePlayerStats(request.getLoserUserId(), request.getLoserUsername(),
                request.getGameId(), false, request.getLoserScore());

        return result;
    }

    @Cacheable(value = "leaderboards", key = "'global_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PagedResponseDto<LeaderboardEntryDto> getGlobalLeaderboard(Pageable pageable) {
        Page<PlayerStats> page = playerStatsRepository.findGlobalLeaderboard(pageable);
        List<LeaderboardEntryDto> content = page.getContent().stream()
                .map(ps -> {
                    Long rank = playerStatsRepository.findGlobalRankByUserId(ps.getUserId());
                    return toLeaderboardDto(ps, rank != null ? rank.intValue() : 0);
                })
                .collect(Collectors.toList());
        return createPagedResponse(page, content);
    }

    @Cacheable(value = "leaderboards", key = "'game_' + #gameId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public PagedResponseDto<LeaderboardEntryDto> getLeaderboardByGame(UUID gameId, Pageable pageable) {
        Page<PlayerStats> page = playerStatsRepository.findLeaderboardByGame(gameId, pageable);
        List<LeaderboardEntryDto> content = page.getContent().stream()
                .map(ps -> {
                    Long rank = playerStatsRepository.findGameRankByUserId(ps.getUserId(), gameId);
                    return toLeaderboardDto(ps, rank != null ? rank.intValue() : 0);
                })
                .collect(Collectors.toList());
        return createPagedResponse(page, content);
    }

    public PagedResponseDto<MatchResult> getMatchHistory(UUID userId, Pageable pageable) {
        Page<MatchResult> page = matchResultRepository.findByWinnerUserIdOrLoserUserIdOrderByPlayedAtDesc(userId, userId, pageable);
        return PagedResponseDto.fromPage(page);
    }

    public PlayerStats getPlayerStats(UUID userId, UUID gameId) {
        return playerStatsRepository.findByUserIdAndGameId(userId, gameId)
                .orElseThrow(() -> new RuntimeException("Stats not found for user " + userId + " on game " + gameId));
    }

    private void updatePlayerStats(UUID userId, String username, UUID gameId, boolean won, int score) {
        PlayerStats stats = playerStatsRepository.findByUserIdAndGameId(userId, gameId)
                .orElse(PlayerStats.builder()
                        .userId(userId)
                        .username(username)
                        .gameId(gameId)
                        .totalMatches(0).wins(0).losses(0).totalScore(0)
                        .build());

        stats.setTotalMatches(stats.getTotalMatches() + 1);
        stats.setTotalScore(stats.getTotalScore() + score);
        if (won) {
            stats.setWins(stats.getWins() + 1);
        } else {
            stats.setLosses(stats.getLosses() + 1);
        }

        playerStatsRepository.save(stats);
    }

    private LeaderboardEntryDto toLeaderboardDto(PlayerStats ps, int rank) {
        double winRate = ps.getTotalMatches() == 0 ? 0.0
                : Math.round(((double) ps.getWins() / ps.getTotalMatches()) * 10000.0) / 100.0;
        return LeaderboardEntryDto.builder()
                .rank(rank)
                .userId(ps.getUserId())
                .username(ps.getUsername())
                .wins(ps.getWins())
                .losses(ps.getLosses())
                .totalMatches(ps.getTotalMatches())
                .totalScore(ps.getTotalScore())
                .winRate(winRate)
                .build();
    }

    private <T> PagedResponseDto<T> createPagedResponse(Page<?> page, List<T> content) {
        return PagedResponseDto.<T>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }
}
