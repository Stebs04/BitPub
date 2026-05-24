package com.bitpub.stats.service;

import com.bitpub.stats.dto.LeaderboardEntryDto;
import com.bitpub.stats.dto.RecordMatchRequest;
import com.bitpub.stats.model.Leaderboard;
import com.bitpub.stats.model.MatchResult;
import com.bitpub.stats.model.PlayerStats;
import com.bitpub.stats.repository.LeaderboardRepository;
import com.bitpub.stats.repository.MatchResultRepository;
import com.bitpub.stats.repository.PlayerStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayerStatsRepository playerStatsRepository;
    private final MatchResultRepository matchResultRepository;
    private final LeaderboardRepository leaderboardRepository;

    /**
     * Registra il risultato di una partita e aggiorna le statistiche di entrambi i giocatori.
     * La deduplica per matchSessionId garantisce idempotenza (sicuro da chiamare più volte).
     */
    @Transactional
    public MatchResult recordMatch(RecordMatchRequest request) {
        // Deduplica: se la sessione è già registrata, ignora
        if (matchResultRepository.existsByMatchSessionId(request.getMatchSessionId())) {
            log.warn("Match session {} already recorded, skipping", request.getMatchSessionId());
            return matchResultRepository
                    .findByWinnerUserIdOrLoserUserIdOrderByPlayedAtDesc(request.getWinnerUserId(), request.getLoserUserId())
                    .stream().findFirst().orElseThrow();
        }

        // Salva il risultato raw
        MatchResult result = MatchResult.builder()
                .matchSessionId(request.getMatchSessionId())
                .gameId(request.getGameId())
                .winnerUserId(request.getWinnerUserId())
                .loserUserId(request.getLoserUserId())
                .winnerScore(request.getWinnerScore())
                .loserScore(request.getLoserScore())
                .build();
        matchResultRepository.save(result);

        // Aggiorna statistiche aggregate del vincitore
        updatePlayerStats(request.getWinnerUserId(), request.getWinnerUsername(),
                request.getGameId(), true, request.getWinnerScore());

        // Aggiorna statistiche aggregate del perdente
        updatePlayerStats(request.getLoserUserId(), request.getLoserUsername(),
                request.getGameId(), false, request.getLoserScore());

        return result;
    }

    /**
     * Restituisce la classifica globale (tutti i giochi).
     */
    public List<LeaderboardEntryDto> getGlobalLeaderboard() {
        AtomicInteger rank = new AtomicInteger(1);
        return playerStatsRepository.findGlobalLeaderboard().stream()
                .map(ps -> toLeaderboardDto(ps, rank.getAndIncrement()))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce la classifica per un gioco specifico.
     */
    public List<LeaderboardEntryDto> getLeaderboardByGame(UUID gameId) {
        AtomicInteger rank = new AtomicInteger(1);
        return playerStatsRepository.findLeaderboardByGame(gameId).stream()
                .map(ps -> toLeaderboardDto(ps, rank.getAndIncrement()))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce lo storico delle partite di un utente.
     */
    public List<MatchResult> getMatchHistory(UUID userId) {
        return matchResultRepository
                .findByWinnerUserIdOrLoserUserIdOrderByPlayedAtDesc(userId, userId);
    }

    /**
     * Restituisce le statistiche di un singolo giocatore per un gioco.
     */
    public PlayerStats getPlayerStats(UUID userId, UUID gameId) {
        return playerStatsRepository.findByUserIdAndGameId(userId, gameId)
                .orElseThrow(() -> new RuntimeException("Stats not found for user " + userId + " on game " + gameId));
    }

    // ---- Private helpers ----

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
}
