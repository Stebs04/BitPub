package com.bitpub.game.controller;

import com.bitpub.contracts.events.BaseSensorEvent;
import com.bitpub.game.service.LiveSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/sessions")
public class InternalSessionController {

    private final LiveSessionService liveSessionService;

    public InternalSessionController(LiveSessionService liveSessionService) {
        this.liveSessionService = liveSessionService;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> receiveEvent(@RequestBody BaseSensorEvent event) {
        liveSessionService.processEvent(event);
        return ResponseEntity.ok().build();
    }
}
