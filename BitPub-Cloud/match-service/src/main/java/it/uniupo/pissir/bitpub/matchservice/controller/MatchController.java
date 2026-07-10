// Autore: Timothy Giolito 20054431
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

    // Recupera la lista delle partite attive. Se l'utente richiedente ha il ruolo di LOCALE_ADMIN,
    // la lista verrà filtrata in modo da includere esclusivamente le partite del proprio locale.
    // L'identificativo del locale viene prelevato dal claim JWT X-User-Locale-Id oppure, in caso
    // di assenza, tramite interrogazione al servizio locale utilizzando l'ID utente.
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
     * Consente a un giocatore di unirsi alla fase di pre-partita (lobby) di un gioco specifico.
     * Nel caso sia il primo giocatore ad accedere, la lobby viene creata nello stato di attesa 
     * (WAITING_FOR_PLAYERS). Qualora si unisca il secondo giocatore, la partita passa 
     * immediatamente allo stato di esecuzione (IN_PROGRESS), e l'evento viene propagato via MQTT 
     * agli iscritti.
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

    /** Restituisce i dettagli di una lobby in attesa di giocatori per un'istanza di gioco, se disponibile. */
    @GetMapping("/lobby/{gameInstanceId}")
    public ResponseEntity<MatchDto> getWaitingLobby(@PathVariable String gameInstanceId) {
        return matchService.getWaitingLobby(gameInstanceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Interrompe forzatamente una partita in corso. L'operazione è riservata agli amministratori 
     * (LOCALE_ADMIN del locale interessato o PLATFORM_ADMIN).
     * Il vincitore viene calcolato automaticamente dal servizio sulla base del punteggio migliore
     * (ad esempio, il punteggio più alto o, nel caso delle freccette, il punteggio minore a partire da 501).
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
     * Registra l'esito finale fornito dal nodo Edge, che è da considerarsi autoritativo rispetto ai 
     * punteggi aggiornati in tempo reale. Il corpo della richiesta deve contenere una mappa con 
     * il formato { "NomeSquadra": punteggio }. Questa procedura aggiorna i punteggi definitivi, 
     * chiude la sessione di gioco e attiva la notifica per il calcolo delle statistiche o del torneo.
     * Operazione interna eseguita dall'Edge verso il Cloud, pertanto non prevede restrizioni di ruolo.
     */
    @PostMapping("/{id}/result")
    public ResponseEntity<MatchDto> reportResult(@PathVariable String id,
            @RequestBody Map<String, Integer> scoresByTeamName) {
        return ResponseEntity.ok(matchService.applyFinalResult(id, scoresByTeamName));
    }

    /**
     * Ricostruisce lo storico e le classifiche a partire dalle partite già concluse.
     * Operazione riservata agli amministratori di piattaforma (PLATFORM_ADMIN), particolarmente utile
     * per il recupero di dati nel caso in cui il servizio di statistiche sia risultato temporaneamente non disponibile.
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
