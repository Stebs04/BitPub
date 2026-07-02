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
import java.util.Set;

@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    // tournament-service non dipende da spring-security (nessun SecurityConfig/@PreAuthorize
    // qui, a differenza di locale-service/statistics-service): il controllo ruolo replica lo
    // stesso pattern header-based gia' usato in MatchController (X-User-Role dal gateway).
    private static final Set<String> TOURNAMENT_MANAGERS = Set.of("PLATFORM_ADMIN", "LOCALE_ADMIN");

    private final TournamentServiceImpl tournamentService;

    private void assertCanManageTournaments(String callerRole) {
        if (callerRole == null || !TOURNAMENT_MANAGERS.contains(callerRole)) {
            throw new BitpubException("Only LOCALE_ADMIN or PLATFORM_ADMIN can manage tournaments", HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping
    public ResponseEntity<TournamentDto> createTournament(@RequestBody TournamentDto tournamentDto,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        assertCanManageTournaments(callerRole);
        return new ResponseEntity<>(tournamentService.createTournament(tournamentDto), HttpStatus.CREATED);
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

    @PutMapping("/{id}/start")
    public ResponseEntity<TournamentDto> startTournament(@PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        assertCanManageTournaments(callerRole);
        return ResponseEntity.ok(tournamentService.startTournament(id));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<TournamentDto> endTournament(@PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        assertCanManageTournaments(callerRole);
        return ResponseEntity.ok(tournamentService.endTournament(id));
    }

    // Iscrizione: aperta a qualunque utente autenticato (in primis il PLAYER). Nessuna
    // restrizione di ruolo qui, cosi' come in precedenza.
    @PostMapping("/{id}/register")
    public ResponseEntity<TournamentRegistrationDto> registerToTournament(
            @PathVariable String id,
            @RequestBody TournamentRegistrationDto registrationDto) {
        return new ResponseEntity<>(tournamentService.registerToTournament(id, registrationDto), HttpStatus.CREATED);
    }

    @GetMapping("/by-player/{playerId}")
    public ResponseEntity<List<TournamentRegistrationDto>> getRegistrationsByPlayer(@PathVariable String playerId) {
        return ResponseEntity.ok(tournamentService.getRegistrationsByParticipant(playerId));
    }
}
