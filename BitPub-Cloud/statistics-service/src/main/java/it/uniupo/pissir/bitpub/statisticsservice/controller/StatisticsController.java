package it.uniupo.pissir.bitpub.statisticsservice.controller;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.GameUsageDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.GlobalStatsDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.LeaderboardEntryDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.MatchResultEvent;
import it.uniupo.pissir.bitpub.statisticsservice.dto.StatisticUpdateRequest;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    // Se il chiamante e' un LOCALE_ADMIN, puo' interrogare solo le statistiche aggregate
    // del proprio locale (entityType=LOCALE, entityId=<il proprio localeId>).
    @GetMapping
    public ResponseEntity<List<AggregateStatisticDto>> getStatistics(
            @RequestParam String entityId,
            @RequestParam String entityType,
            @RequestHeader(value = "X-User-Id", required = false) String callerId,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        if ("LOCALE_ADMIN".equals(callerRole)) {
            String ownLocaleId = callerLocaleId != null ? callerLocaleId : statisticsService.resolveAdminLocaleId(callerId);
            if (!"LOCALE".equals(entityType) || ownLocaleId == null || !ownLocaleId.equals(entityId)) {
                throw new BitpubException("LOCALE_ADMIN can only view statistics of their own locale", HttpStatus.FORBIDDEN);
            }
        }
        return ResponseEntity.ok(statisticsService.getStatisticsByEntity(entityId, entityType));
    }

    // Monitoraggio dell'intero sistema e statistiche aggregate mondiali: riservato a PLATFORM_ADMIN.
    @GetMapping("/global")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<GlobalStatsDto> getGlobalOverview() {
        return ResponseEntity.ok(statisticsService.getGlobalOverview());
    }

    @PostMapping("/update")
    public ResponseEntity<AggregateStatisticDto> updateStatistic(@RequestBody StatisticUpdateRequest request) {
        return ResponseEntity.ok(statisticsService.updateStatistic(request));
    }

    /**
     * Backfill invocato dal match-service: azzera e ricostruisce la leaderboard dai match conclusi.
     * ponytail: endpoint interno trusted (stesso pattern di /match-result); l'autorizzazione
     * PLATFORM_ADMIN e' applicata dal match-service che riceve il JWT dal gateway.
     */
    @PostMapping("/leaderboard/rebuild")
    public ResponseEntity<Integer> rebuildLeaderboard(@RequestBody List<MatchResultEvent> events) {
        return ResponseEntity.ok(statisticsService.rebuildLeaderboard(events));
    }

    /**
     * Metrica "Giochi piu' utilizzati in un locale". Aperta a tutti i ruoli in lettura;
     * un LOCALE_ADMIN puo' consultare solo il proprio locale.
     */
    @GetMapping("/locale/{localeId}/games-usage")
    public ResponseEntity<List<GameUsageDto>> getMostUsedGames(
            @PathVariable String localeId,
            @RequestHeader(value = "X-User-Id", required = false) String callerId,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        if ("LOCALE_ADMIN".equals(callerRole)) {
            String ownLocaleId = callerLocaleId != null ? callerLocaleId : statisticsService.resolveAdminLocaleId(callerId);
            if (ownLocaleId == null || !ownLocaleId.equals(localeId)) {
                throw new BitpubException("LOCALE_ADMIN can only view statistics of their own locale", HttpStatus.FORBIDDEN);
            }
        }
        return ResponseEntity.ok(statisticsService.getMostUsedGamesByLocale(localeId));
    }

    /**
     * Statistiche personali del PLAYER: le sue righe di leaderboard su tutte le tipologie di gioco,
     * interrogate direttamente dal DB (una sola query) invece di scorrere e filtrare in memoria.
     */
    @GetMapping("/leaderboard/me/{playerName}")
    public ResponseEntity<List<LeaderboardEntryDto>> getMyStatistics(@PathVariable String playerName) {
        return ResponseEntity.ok(statisticsService.getMyLeaderboardEntries(playerName));
    }
}
