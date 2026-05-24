package com.bitpub.stats.controller;

import com.bitpub.stats.dto.LeaderboardEntryDto;
import com.bitpub.stats.dto.RecordMatchRequest;
import com.bitpub.stats.model.MatchResult;
import com.bitpub.stats.model.PlayerStats;
import com.bitpub.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * Registra il risultato di una partita (chiamato dall'Edge-Sync Service o dal Game Service).
     * Idempotente: la stessa matchSessionId non viene processata due volte.
     */
    @PostMapping("/matches")
    public ResponseEntity<MatchResult> recordMatch(@RequestBody RecordMatchRequest request) {
        return ResponseEntity.ok(statsService.recordMatch(request));
    }

    /**
     * Classifica globale (tutti i giochi, ordinata per vittorie).
     */
    @GetMapping("/leaderboard/global")
    public ResponseEntity<List<LeaderboardEntryDto>> getGlobalLeaderboard() {
        return ResponseEntity.ok(statsService.getGlobalLeaderboard());
    }

    /**
     * Classifica per un gioco specifico.
     */
    @GetMapping("/leaderboard/game/{gameId}")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboardByGame(@PathVariable UUID gameId) {
        return ResponseEntity.ok(statsService.getLeaderboardByGame(gameId));
    }

    /**
     * Storico partite di un utente.
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<MatchResult>> getMatchHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(statsService.getMatchHistory(userId));
    }

    /**
     * Statistiche di un giocatore per un gioco specifico.
     */
    @GetMapping("/players/{userId}/game/{gameId}")
    public ResponseEntity<PlayerStats> getPlayerStats(@PathVariable UUID userId, @PathVariable UUID gameId) {
        return ResponseEntity.ok(statsService.getPlayerStats(userId, gameId));
    }
}
