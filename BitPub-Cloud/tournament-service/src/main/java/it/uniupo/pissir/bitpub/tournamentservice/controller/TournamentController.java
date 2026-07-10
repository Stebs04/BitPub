/**
 * Autore: Stefano Bellan Matricola 20054330
 */
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

/**
 * Controller REST che espone le API per la gestione dei tornei.
 * Gestisce la creazione, l'aggiornamento e l'iscrizione ai tornei, delegando la logica di business al servizio.
 */
@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    // Il servizio non include dipendenze da Spring Security. La validazione dei ruoli viene
    // gestita manualmente leggendo l'header HTTP 'X-User-Role' iniettato dal gateway.
    // La gestione amministrativa dei tornei è riservata esclusivamente ai gestori dei locali (LOCALE_ADMIN).
    private final TournamentServiceImpl tournamentService;

    /** Verifica che il chiamante sia un LOCALE_ADMIN e restituisce il suo ID locale estratto dal token. */
    private String requireLocaleAdmin(String callerRole, String callerLocaleId) {
        if (!"LOCALE_ADMIN".equals(callerRole)) {
            throw new BitpubException("Solo un LOCALE_ADMIN puo' gestire i tornei", HttpStatus.FORBIDDEN);
        }
        if (callerLocaleId == null) {
            throw new BitpubException("Locale del LOCALE_ADMIN non determinabile", HttpStatus.FORBIDDEN);
        }
        return callerLocaleId;
    }

    /** Verifica che il torneo richiesto sia effettivamente associato al locale del chiamante. */
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
        // Il torneo viene automaticamente associato al locale gestito dall'amministratore che effettua la richiesta
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
        tournamentDto.setLocaleIds(List.of(locale)); // Mantiene invariata l'associazione al locale corretto
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

    // L'avvio manuale del torneo è stato rimosso in favore di una gestione automatizzata.
    // Il tabellone viene generato e il torneo avviato non appena si raggiunge il numero massimo di iscritti.

    @PutMapping("/{id}/end")
    public ResponseEntity<TournamentDto> endTournament(@PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole,
            @RequestHeader(value = "X-User-Locale-Id", required = false) String callerLocaleId) {
        assertOwns(id, requireLocaleAdmin(callerRole, callerLocaleId));
        return ResponseEntity.ok(tournamentService.endTournament(id));
    }

    // Endpoint dedicato all'iscrizione. Solo i giocatori (PLAYER) possono iscriversi, gli amministratori non partecipano.
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

    // Anche la generazione manuale del tabellone è stata deprecata in favore della logica automatica implementata in fase di registrazione.

    /**
     * Endpoint interno utilizzato dal match-service per verificare se un giocatore è autorizzato a partecipare a un incontro specifico.
     * Blocca eventuali tentativi di connessione da parte di giocatori non abbinati allo scontro nel tabellone.
     */
    @GetMapping("/matches/{matchId}/authorize")
    public ResponseEntity<Boolean> authorizeBracketPlayer(@PathVariable String matchId,
            @RequestParam String playerId) {
        return ResponseEntity.ok(tournamentService.isPlayerInBracketMatch(matchId, playerId));
    }

    /**
     * Endpoint di servizio richiamato dal match-service al termine di una partita.
     * Memorizza il risultato dello scontro e fa progredire il vincitore nel tabellone a eliminazione diretta.
     * Non richiede validazione del ruolo in quanto si tratta di una comunicazione inter-servizio protetta.
     */
    @PostMapping("/matches/{matchId}/result")
    public ResponseEntity<Void> reportBracketResult(@PathVariable String matchId,
            @RequestParam String winnerId,
            @RequestParam(required = false) String stats) {
        tournamentService.updateMatchResult(matchId, winnerId, stats);
        return ResponseEntity.ok().build();
    }

    // L'inserimento manuale dell'esito da parte di un amministratore ora sfrutta il flusso MQTT tramite Edge
    // e non è più esposto come chiamata REST diretta in questo controller.
}
