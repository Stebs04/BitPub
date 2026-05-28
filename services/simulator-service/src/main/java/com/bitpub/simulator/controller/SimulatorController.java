package com.bitpub.simulator.controller;

import com.bitpub.simulator.service.SimulatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
        return switch (gameType.toUpperCase()) {
            case "TABLE_FOOTBALL" -> ResponseEntity.ok(simulatorService.simulateCalciobalilla());
            case "POOL" -> ResponseEntity.ok(simulatorService.simulateBiliardo());
            case "DARTS" -> ResponseEntity.ok(simulatorService.simulateFreccette());
            default -> ResponseEntity.badRequest().body("Game type not supported");
        };
    }
}
