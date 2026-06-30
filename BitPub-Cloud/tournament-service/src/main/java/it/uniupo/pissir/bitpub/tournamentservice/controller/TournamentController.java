package it.uniupo.pissir.bitpub.tournamentservice.controller;

import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRegistrationDto;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping
    public ResponseEntity<TournamentDto> createTournament(@RequestBody TournamentDto tournamentDto) {
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
    public ResponseEntity<TournamentDto> startTournament(@PathVariable String id) {
        return ResponseEntity.ok(tournamentService.startTournament(id));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<TournamentDto> endTournament(@PathVariable String id) {
        return ResponseEntity.ok(tournamentService.endTournament(id));
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<TournamentRegistrationDto> registerToTournament(
            @PathVariable String id,
            @RequestBody TournamentRegistrationDto registrationDto) {
        return new ResponseEntity<>(tournamentService.registerToTournament(id, registrationDto), HttpStatus.CREATED);
    }
}
