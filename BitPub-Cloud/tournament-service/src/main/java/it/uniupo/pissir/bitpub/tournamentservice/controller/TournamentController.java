package it.uniupo.pissir.bitpub.tournamentservice.controller;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRegistrationDto;
import it.uniupo.pissir.bitpub.tournamentservice.service.impl.TournamentServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    // tournament-service non dipende da spring-security (nessun SecurityConfig/@PreAuthorize
    // qui, a differenza di locale-service/statistics-service): il controllo ruolo replica lo
    // stesso pattern header-based gia' usato in MatchController (X-User-Role dal gateway).
    // I tornei sono gestiti SOLO dal LOCALE_ADMIN, ognuno per il proprio locale.
    private final TournamentServiceImpl tournamentService;

    /** Solo il LOCALE_ADMIN gestisce i tornei; ritorna il suo localeId (dal claim del gateway). */
    private String requireLocaleAdmin(String callerRole, String callerLocaleId) {
        if (!"LOCALE_ADMIN".equals(callerRole)) {
            throw new BitpubException("Solo un LOCALE_ADMIN puo' gestire i tornei", HttpStatus.FORBIDDEN);
        }
        if (callerLocaleId == null) {
            throw new BitpubException("Locale del LOCALE_ADMIN non determinabile", HttpStatus.FORBIDDEN);
        }
        return callerLocaleId;
    }

    /** Il torneo deve appartenere al locale del chiamante. */
    private void assertOwns(String id, String localeId) {
        List<String> localeIds = tournamentService.getTournament(id).getLocaleIds();
        if (localeIds == null || !localeIds.contains(localeId)) {
            throw new BitpubException("Il torneo non appartiene al tuo locale", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping
    public ResponseEntity<TournamentDto> createTournament(@RequestBody TournamentDto tournamentDto,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        String locale = requireLocaleAdmin(callerRole, callerLocaleId);
        // Il locale del torneo e' sempre quello del LOCALE_ADMIN: non e' scelto dal client.
        tournamentDto.setLocaleIds(List.of(locale));
        return new ResponseEntity<>(tournamentService.createTournament(tournamentDto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TournamentDto> updateTournament(@PathVariable String id,
            @RequestBody TournamentDto tournamentDto,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        String locale = requireLocaleAdmin(callerRole, callerLocaleId);
        assertOwns(id, locale);
        tournamentDto.setLocaleIds(List.of(locale)); // il locale resta il proprio
        return ResponseEntity.ok(tournamentService.updateTournament(id, tournamentDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        String locale = requireLocaleAdmin(callerRole, callerLocaleId);
        assertOwns(id, locale);
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentDto> getTournament(@PathVariable String id) {
        return ResponseEntity.ok(tournamentService.getTournament(id));
    }

    @GetMapping
    public ResponseEntity<List<TournamentDto>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TournamentDto>> getActiveTournaments() {
        return ResponseEntity.ok(tournamentService.getActiveTournaments());
    }

    // Avvio manuale rimosso: il torneo si avvia da solo generando il tabellone al raggiungimento
    // di maxParticipants (vedi registerToTournament -> generateBracket). L'admin non lo avvia piu'.

    @PutMapping("/{id}/end")
    public ResponseEntity<TournamentDto> endTournament(@PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        assertOwns(id, requireLocaleAdmin(callerRole, callerLocaleId));
        return ResponseEntity.ok(tournamentService.endTournament(id));
    }

    // Iscrizione riservata ai soli PLAYER: gli admin gestiscono i tornei, non vi partecipano.
    @PostMapping("/{id}/register")
    public ResponseEntity<TournamentRegistrationDto> registerToTournament(
            @PathVariable String id,
            @RequestBody TournamentRegistrationDto registrationDto,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        if (!"PLAYER".equals(callerRole)) {
            throw new BitpubException("Solo i PLAYER possono iscriversi ai tornei", HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(tournamentService.registerToTournament(id, registrationDto), HttpStatus.CREATED);
    }

    @GetMapping("/by-player/{playerId}")
    public ResponseEntity<List<TournamentRegistrationDto>> getRegistrationsByPlayer(@PathVariable String playerId) {
        return ResponseEntity.ok(tournamentService.getRegistrationsByParticipant(playerId));
    }

    // Generazione manuale del tabellone rimossa: avviene in automatico al raggiungimento di
    // maxParticipants durante l'iscrizione. Nessun endpoint di avvio manuale esposto.

    /**
     * Controllo interno (chiamato da match-service): il player e' abbinato in questo scontro
     * del tabellone? Impedisce ai giocatori non abbinati di connettersi alla partita di torneo.
     */
    @GetMapping("/matches/{matchId}/authorize")
    public ResponseEntity<Boolean> authorizeBracketPlayer(@PathVariable String matchId,
            @RequestParam String playerId) {
        return ResponseEntity.ok(tournamentService.isPlayerInBracketMatch(matchId, playerId));
    }

    /**
     * Interno (chiamato da match-service a fine partita): registra il vincitore dello scontro
     * e fa avanzare il vincitore al match successivo del tabellone. Nessun gate di ruolo (come
     * /authorize): e' una chiamata service-to-service, non esposta ai client via gateway con auth.
     */
    @PostMapping("/matches/{matchId}/result")
    public ResponseEntity<Void> reportBracketResult(@PathVariable String matchId,
            @RequestParam String winnerId,
            @RequestParam(required = false) String stats) {
        tournamentService.updateMatchResult(matchId, winnerId, stats);
        return ResponseEntity.ok().build();
    }

    /** Registra il vincitore e le statistiche di uno scontro del tabellone. Solo LOCALE_ADMIN proprietario. */
    @PutMapping("/{id}/matches/{matchId}/result")
    public ResponseEntity<TournamentDto> updateMatchResult(@PathVariable String id, @PathVariable String matchId,
            @RequestParam String winnerId,
            @RequestParam(required = false) String stats,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        assertOwns(id, requireLocaleAdmin(callerRole, callerLocaleId));
        return ResponseEntity.ok(tournamentService.updateMatchResult(matchId, winnerId, stats));
    }
}
