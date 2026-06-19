package com.bitpub.game.controller;

import com.bitpub.game.service.LiveSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sessions/active")
@PreAuthorize("isAuthenticated()")
public class SessionController {

    private final LiveSessionService liveSessionService;

    public SessionController(LiveSessionService liveSessionService) {
        this.liveSessionService = liveSessionService;
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getActiveSessionScore(@PathVariable String sessionId) {
        com.bitpub.game.model.MatchSession match = liveSessionService.getActiveSession(sessionId);
        
        if (match == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "score1", String.valueOf(match.getScore1()),
                "score2", String.valueOf(match.getScore2()),
                "player1", "Player 1",
                "player2", "Player 2",
                "status", match.getStatus()
        ));
    }
}
