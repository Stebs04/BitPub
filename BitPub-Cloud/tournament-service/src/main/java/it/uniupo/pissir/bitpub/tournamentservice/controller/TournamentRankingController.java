package it.uniupo.pissir.bitpub.tournamentservice.controller;

import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRankingDto;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}/rankings")
@RequiredArgsConstructor
public class TournamentRankingController {

    private final TournamentRankingService rankingService;

    @GetMapping
    public ResponseEntity<List<TournamentRankingDto>> getTournamentRankings(@PathVariable String tournamentId) {
        return ResponseEntity.ok(rankingService.getTournamentRankings(tournamentId));
    }

    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeRankings(@PathVariable String tournamentId) {
        rankingService.initializeRankingsForTournament(tournamentId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/score")
    public ResponseEntity<TournamentRankingDto> updateRankingScore(
            @PathVariable String tournamentId,
            @RequestParam String participantId,
            @RequestParam int scoreDelta,
            @RequestParam boolean isWin) {
        return ResponseEntity.ok(rankingService.updateRankingScore(tournamentId, participantId, scoreDelta, isWin));
    }
}
