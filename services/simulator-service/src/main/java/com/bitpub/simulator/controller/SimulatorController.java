package com.bitpub.simulator.controller;

import com.bitpub.simulator.service.SimulatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulators")
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @PostMapping("/simulate/{gameType}")
    @PreAuthorize("hasAnyRole('PLAYER', 'PLATFORM_ADMIN')")
    public ResponseEntity<?> simulateMatch(@PathVariable String gameType) {
        String sessionId;
        switch (gameType.toUpperCase()) {
            case "TABLE_FOOTBALL" -> sessionId = simulatorService.simulateCalciobalilla();
            case "POOL" -> sessionId = simulatorService.simulateBiliardo();
            case "DARTS" -> sessionId = simulatorService.simulateFreccette();
            default -> {
                return ResponseEntity.badRequest().body("Game type not supported");
            }
        }
        return ResponseEntity.accepted().body(Map.of(
                "message", "Simulazione avviata, dati in arrivo",
                "sessionId", sessionId
        ));
    }
}
