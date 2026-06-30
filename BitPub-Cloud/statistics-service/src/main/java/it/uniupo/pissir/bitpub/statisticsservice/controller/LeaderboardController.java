package it.uniupo.pissir.bitpub.statisticsservice.controller;

import it.uniupo.pissir.bitpub.statisticsservice.dto.LeaderboardEntryDto;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exposes the real leaderboard split by game type.
 * GET /api/v1/statistics/leaderboard/{gameTypeId}
 * Returns entries ordered by wins DESC, totalPoints DESC.
 */
@RestController
@RequestMapping("/api/v1/statistics/leaderboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LeaderboardController {

    private final StatisticsService statisticsService;

    @GetMapping("/{gameTypeId}")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @PathVariable("gameTypeId") String gameTypeId) {
        List<LeaderboardEntryDto> entries = statisticsService.getLeaderboard(gameTypeId);
        return ResponseEntity.ok(entries);
    }
}
