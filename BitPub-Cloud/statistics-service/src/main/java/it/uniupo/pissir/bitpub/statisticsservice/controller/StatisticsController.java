/**
 * Autore: Stefano Bellan Matricola 20054330
 */
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

    // Endpoint per recuperare le statistiche aggregate basate sull'entità (es. giocatore o locale).
    // Nel caso in cui la richiesta provenga da un amministratore di locale (LOCALE_ADMIN), 
    // viene applicata una restrizione per limitare la visibilità ai soli dati del locale di competenza.
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

    // Vista globale delle statistiche della piattaforma (es. totale utenti, totale locali). Accesso riservato esclusivamente agli amministratori di piattaforma.
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
     * Endpoint di ricalcolo (backfill) richiamato internamente dal match-service.
     * Azzeramento totale e successiva ricostruzione della classifica basata sullo storico delle partite concluse.
     * Essendo una chiamata inter-servizio fidata, il controllo dei ruoli è già stato gestito a monte dal match-service.
     */
    @PostMapping("/leaderboard/rebuild")
    public ResponseEntity<Integer> rebuildLeaderboard(@RequestBody List<MatchResultEvent> events) {
        return ResponseEntity.ok(statisticsService.rebuildLeaderboard(events));
    }

    /**
     * Espone le metriche relative ai giochi maggiormente utilizzati all'interno di uno specifico locale.
     * La lettura è pubblica per i giocatori, mentre gli amministratori di locale rimangono vincolati al proprio spazio.
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
     * Recupera le statistiche personali complete per un singolo giocatore, raccogliendo i posizionamenti 
     * in classifica attraverso tutte le categorie di gioco tramite un'unica query ottimizzata.
     */
    @GetMapping("/leaderboard/me/{playerName}")
    public ResponseEntity<List<LeaderboardEntryDto>> getMyStatistics(@PathVariable String playerName) {
        return ResponseEntity.ok(statisticsService.getMyLeaderboardEntries(playerName));
    }
}
