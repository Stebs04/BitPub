package it.uniupo.pissir.bitpub.statisticsservice.controller;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    // Stessi gameTypeId gia' usati dalla LeaderboardPage (GAME_TYPE_MAP) per le 3 tipologie
    // di gioco della demo: la leaderboard e' indicizzata per gameTypeId, non esiste una query
    // "tutti i game type" lato statistics-service.
    private static final List<String> KNOWN_GAME_TYPE_IDS = List.of("foosball", "darts", "billiards");

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
     * Called by match-service when a match completes.
     * Updates the leaderboard for winner and loser.
     */
    @PostMapping("/match-result")
    public ResponseEntity<Void> recordMatchResult(@RequestBody MatchResultEvent event) {
        statisticsService.recordMatchResult(event);
        return ResponseEntity.ok().build();
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
     * Statistiche personali del PLAYER: le sue righe di leaderboard su tutte le tipologie
     * di gioco della demo, in un'unica chiamata (riusa il servizio leaderboard esistente,
     * nessuna nuova query aggiunta al livello service/repository).
     */
    @GetMapping("/leaderboard/me/{playerName}")
    public ResponseEntity<List<LeaderboardEntryDto>> getMyStatistics(@PathVariable String playerName) {
        List<LeaderboardEntryDto> mine = KNOWN_GAME_TYPE_IDS.stream()
                .flatMap(gameTypeId -> statisticsService.getLeaderboard(gameTypeId).stream())
                .filter(entry -> entry.getPlayerName() != null && entry.getPlayerName().equalsIgnoreCase(playerName))
                .collect(Collectors.toList());
        return ResponseEntity.ok(mine);
    }
}
