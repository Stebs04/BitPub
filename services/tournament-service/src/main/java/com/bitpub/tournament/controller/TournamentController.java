package com.bitpub.tournament.controller;

import com.bitpub.tournament.dto.CreateTournamentRequest;
import com.bitpub.tournament.dto.RegisterParticipantRequest;
import com.bitpub.tournament.dto.SubmitResultRequest;
import com.bitpub.tournament.model.Tournament;
import com.bitpub.tournament.model.TournamentMatch;
import com.bitpub.tournament.model.TournamentParticipant;
import com.bitpub.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @GetMapping
    public ResponseEntity<List<Tournament>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tournament> getTournamentById(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }

    @PostMapping
    public ResponseEntity<Tournament> createTournament(@RequestBody CreateTournamentRequest request) {
        return ResponseEntity.ok(tournamentService.createTournament(request));
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<TournamentParticipant> registerParticipant(
            @PathVariable UUID id,
            @RequestBody RegisterParticipantRequest request) {
        return ResponseEntity.ok(tournamentService.registerParticipant(id, request));
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<TournamentParticipant>> getParticipants(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getLeaderboard(id));
    }

    @PostMapping("/{id}/bracket/generate")
    public ResponseEntity<List<TournamentMatch>> generateBracket(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.generateBracket(id));
    }

    @GetMapping("/{id}/bracket")
    public ResponseEntity<List<TournamentMatch>> getBracket(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getBracket(id));
    }

    @PostMapping("/matches/result")
    public ResponseEntity<TournamentMatch> submitResult(@RequestBody SubmitResultRequest request) {
        return ResponseEntity.ok(tournamentService.submitResult(request));
    }
}
