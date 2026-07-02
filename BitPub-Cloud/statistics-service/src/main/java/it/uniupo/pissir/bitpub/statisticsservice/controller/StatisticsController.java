package it.uniupo.pissir.bitpub.statisticsservice.controller;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.MatchResultEvent;
import it.uniupo.pissir.bitpub.statisticsservice.dto.StatisticUpdateRequest;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService statisticsService;

    // Se il chiamante e' un LOCALE_ADMIN, puo' interrogare solo le statistiche aggregate
    // del proprio locale (entityType=LOCALE, entityId=<il proprio localeId>).
    @GetMapping
    public ResponseEntity<List<AggregateStatisticDto>> getStatistics(
            @RequestParam String entityId,
            @RequestParam String entityType,
            @RequestHeader(value = "X-User-Id", required = false) String callerId,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        if ("LOCALE_ADMIN".equals(callerRole)) {
            String ownLocaleId = statisticsService.resolveAdminLocaleId(callerId);
            if (!"LOCALE".equals(entityType) || ownLocaleId == null || !ownLocaleId.equals(entityId)) {
                throw new BitpubException("LOCALE_ADMIN can only view statistics of their own locale", HttpStatus.FORBIDDEN);
            }
        }
        return ResponseEntity.ok(statisticsService.getStatisticsByEntity(entityId, entityType));
    }

    @PostMapping("/update")
    public ResponseEntity<AggregateStatisticDto> updateStatistic(@RequestBody StatisticUpdateRequest request) {
        return ResponseEntity.ok(statisticsService.updateStatistic(request));
    }

    /**
     * Called by match-service when a match completes.
     * Updates the leaderboard for winner and loser.
     */
    @PostMapping("/match-result")
    public ResponseEntity<Void> recordMatchResult(@RequestBody MatchResultEvent event) {
        statisticsService.recordMatchResult(event);
        return ResponseEntity.ok().build();
    }
}
