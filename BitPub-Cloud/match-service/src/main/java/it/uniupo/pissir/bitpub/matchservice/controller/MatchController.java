package it.uniupo.pissir.bitpub.matchservice.controller;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import it.uniupo.pissir.bitpub.matchservice.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchDto> startMatch(@RequestBody StartMatchRequestDto request) {
        MatchDto match = matchService.startMatch(request);
        return new ResponseEntity<>(match, HttpStatus.CREATED);
    }

    @GetMapping("/active")
    public ResponseEntity<List<MatchDto>> getActiveMatches() {
        return ResponseEntity.ok(matchService.getActiveMatches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDto> getMatch(@PathVariable String id) {
        return ResponseEntity.ok(matchService.getMatch(id));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<MatchDto> endMatch(@PathVariable String id) {
        return ResponseEntity.ok(matchService.endMatch(id));
    }

    /**
     * Receives sensor events forwarded from the Edge app via REST.
     * Handles all game types: calciobalilla (GOAL), freccette (DART_HIT), biliardo (BALL_POCKETED).
     */
    @PostMapping("/events")
    public ResponseEntity<Void> receiveSensorEvent(@RequestBody SensorEvent event) {
        log.info("Received sensor event via REST: type={}, gameInstanceId={}", event.getSensorType(), event.getGameInstanceId());
        matchService.processSensorEvent(event);
        return ResponseEntity.ok().build();
    }
}
