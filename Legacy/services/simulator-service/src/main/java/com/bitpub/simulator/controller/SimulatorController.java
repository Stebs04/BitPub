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
    public ResponseEntity<String> simulateMatch(@PathVariable String gameType) {
        switch (gameType.toUpperCase()) {
            case "TABLE_FOOTBALL" -> simulatorService.simulateCalciobalilla();
            case "POOL" -> simulatorService.simulateBiliardo();
            case "DARTS" -> simulatorService.simulateFreccette();
            default -> {
                return ResponseEntity.badRequest().body("Game type not supported");
            }
        }
        return ResponseEntity.accepted().body("Simulazione avviata, dati in arrivo sui topic MQTT");
    }
}
