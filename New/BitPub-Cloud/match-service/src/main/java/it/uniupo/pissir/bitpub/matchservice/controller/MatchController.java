package it.uniupo.pissir.bitpub.matchservice.controller;

import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import it.uniupo.pissir.bitpub.matchservice.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchDto> startMatch(@RequestBody StartMatchRequestDto request) {
        MatchDto match = matchService.startMatch(request);
        return new ResponseEntity<>(match, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDto> getMatch(@PathVariable String id) {
        return ResponseEntity.ok(matchService.getMatch(id));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<MatchDto> endMatch(@PathVariable String id) {
        return ResponseEntity.ok(matchService.endMatch(id));
    }
}
