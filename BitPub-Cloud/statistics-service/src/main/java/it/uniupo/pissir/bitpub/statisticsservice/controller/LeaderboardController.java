package it.uniupo.pissir.bitpub.statisticsservice.controller;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.statisticsservice.dto.LeaderboardEntryDto;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    /**
     * Leaderboard filtrata per locale, per il pannello del LOCALE_ADMIN.
     * Se il chiamante e' un LOCALE_ADMIN, localeId deve corrispondere al proprio locale.
     */
    @GetMapping("/locale/{localeId}/{gameTypeId}")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboardByLocale(
            @PathVariable("localeId") String localeId,
            @PathVariable("gameTypeId") String gameTypeId,
            @RequestHeader(value = "X-User-Id", required = false) String callerId,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        if ("LOCALE_ADMIN".equals(callerRole)) {
            String ownLocaleId = statisticsService.resolveAdminLocaleId(callerId);
            if (ownLocaleId == null || !ownLocaleId.equals(localeId)) {
                throw new BitpubException("LOCALE_ADMIN can only view statistics of their own locale", HttpStatus.FORBIDDEN);
            }
        }
        return ResponseEntity.ok(statisticsService.getLeaderboardByLocale(gameTypeId, localeId));
    }
}
