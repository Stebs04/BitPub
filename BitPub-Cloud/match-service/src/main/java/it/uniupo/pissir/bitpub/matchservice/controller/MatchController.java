package it.uniupo.pissir.bitpub.matchservice.controller;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.matchservice.dto.JoinLobbyRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import it.uniupo.pissir.bitpub.matchservice.service.impl.MatchServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
public class MatchController {

    private final MatchServiceImpl matchService;

    @PostMapping
    public ResponseEntity<MatchDto> startMatch(@RequestBody StartMatchRequestDto request) {
        MatchDto match = matchService.startMatch(request);
        return new ResponseEntity<>(match, HttpStatus.CREATED);
    }

    // Se il chiamante e' un LOCALE_ADMIN, la lista viene sempre limitata al proprio locale
    // (dal claim JWT X-User-Locale-Id inoltrato dal gateway, o in fallback via user-id ->
    // locale-service se il token non lo contiene ancora), a prescindere dal parametro localeId passato.
    @GetMapping("/active")
    public ResponseEntity<List<MatchDto>> getActiveMatches(
            @RequestParam(required = false) String localeId,
            @RequestHeader(value = "X-User-Id", required = false) String callerId,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        if ("LOCALE_ADMIN".equals(callerRole)) {
            String ownLocaleId = callerLocaleId != null ? callerLocaleId : matchService.resolveAdminLocaleId(callerId);
            return ResponseEntity.ok(matchService.getActiveMatchesByLocale(ownLocaleId));
        }
        if (localeId != null) {
            return ResponseEntity.ok(matchService.getActiveMatchesByLocale(localeId));
        }
        return ResponseEntity.ok(matchService.getActiveMatches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDto> getMatch(@PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String callerId,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        MatchDto match = matchService.getMatch(id);
        matchService.assertMatchLocaleAccess(match.getLocaleId(), callerId, callerRole, callerLocaleId);
        return ResponseEntity.ok(match);
    }

    @GetMapping("/by-player/{playerId}")
    public ResponseEntity<List<MatchDto>> getMatchesByPlayer(@PathVariable String playerId) {
        return ResponseEntity.ok(matchService.getMatchesByPlayer(playerId));
    }

    /**
     * Matchmaking PLAYER: entra in lobby su una gameInstance. Se e' il primo giocatore,
     * crea la lobby in stato WAITING_FOR_PLAYERS; se e' il secondo, la partita passa
     * IN_PROGRESS in tempo reale (evento propagato via MQTT sul topic di match-state).
     */
    @PostMapping("/lobby")
    public ResponseEntity<MatchDto> joinLobby(@RequestBody JoinLobbyRequestDto request,
            @RequestHeader(value = "X-User-Id", required = false) String callerId) {
        if (callerId == null || request.getGameInstanceId() == null || request.getUsername() == null) {
            throw new BitpubException("gameInstanceId, username e utente autenticato sono obbligatori", HttpStatus.BAD_REQUEST);
        }
        MatchDto match = matchService.joinLobby(request, callerId);
        return ResponseEntity.ok(match);
    }

    /** Ritorna la lobby in attesa di un secondo giocatore su una gameInstance, se presente. */
    @GetMapping("/lobby/{gameInstanceId}")
    public ResponseEntity<MatchDto> getWaitingLobby(@PathVariable String gameInstanceId) {
        return matchService.getWaitingLobby(gameInstanceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Termina subito una partita in corso (LOCALE_ADMIN del locale o PLATFORM_ADMIN).
     * Il vincitore e' calcolato dal match-service in base al punteggio piu' alto
     * (piu' basso a freccette, dove si parte da 501 e si scende a 0).
     */
    @PostMapping("/{id}/end")
    public ResponseEntity<MatchDto> endMatch(@PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String callerId,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        if (!"LOCALE_ADMIN".equals(callerRole) && !"PLATFORM_ADMIN".equals(callerRole)) {
            throw new BitpubException("Solo un LOCALE_ADMIN o PLATFORM_ADMIN puo' terminare una partita", HttpStatus.FORBIDDEN);
        }
        MatchDto match = matchService.getMatch(id);
        matchService.assertMatchLocaleAccess(match.getLocaleId(), callerId, callerRole, callerLocaleId);
        return ResponseEntity.ok(matchService.endMatch(id));
    }

    /**
     * Esito finale riportato dall'Edge (autoritativo sul punteggio live). Body = { "TeamName": score }.
     * Applica i punteggi, chiude la partita e notifica statistiche/torneo. Idempotente sul matchId.
     * Chiamata interna Edge -> Cloud (non passa dal gateway): nessun ruolo richiesto.
     */
    @PostMapping("/{id}/result")
    public ResponseEntity<MatchDto> reportResult(@PathVariable String id,
            @RequestBody Map<String, Integer> scoresByTeamName) {
        return ResponseEntity.ok(matchService.applyFinalResult(id, scoresByTeamName));
    }

    /**
     * Backfill statistiche (PLATFORM_ADMIN): ricostruisce la leaderboard dallo storico dei match
     * gia' conclusi, recuperando quelli terminati mentre lo statistics-service era irraggiungibile.
     */
    @PostMapping("/admin/backfill-stats")
    public ResponseEntity<Integer> backfillStats(
            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        if (!"PLATFORM_ADMIN".equals(callerRole)) {
            throw new BitpubException("Solo un PLATFORM_ADMIN puo' rigenerare le statistiche", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(matchService.backfillStatistics());
    }
}
