/**
 * Autore: Stefano Bellan Matricola 20054330
 */
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
 * Controller REST dedicato all'esposizione delle classifiche, suddivise per tipologia di gioco.
 * L'endpoint restituisce le posizioni ordinate primariamente per numero di vittorie decrescenti,
 * seguite dai punti totali in caso di parità.
 */
@RestController
@RequestMapping("/api/v1/statistics/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final StatisticsService statisticsService;

    @GetMapping("/{gameTypeId}")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @PathVariable("gameTypeId") String gameTypeId) {
        List<LeaderboardEntryDto> entries = statisticsService.getLeaderboard(gameTypeId);
        return ResponseEntity.ok(entries);
    }

    /**
     * Restituisce la classifica globale filtrata per uno specifico locale. Questo endpoint è primariamente utilizzato
     * all'interno del pannello di controllo degli amministratori di locale (LOCALE_ADMIN).
     * In questo scenario viene applicato un controllo di sicurezza per garantire che l'amministratore 
     * possa visualizzare esclusivamente i dati relativi al proprio locale.
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
