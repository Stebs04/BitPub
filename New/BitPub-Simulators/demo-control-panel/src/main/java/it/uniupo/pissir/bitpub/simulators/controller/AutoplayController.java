package it.uniupo.pissir.bitpub.simulators.controller;

import it.uniupo.pissir.bitpub.simulators.service.AutoplayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autoplay")
@CrossOrigin(origins = "*")
public class AutoplayController {

    private final AutoplayService autoplayService;

    public AutoplayController(AutoplayService autoplayService) {
        this.autoplayService = autoplayService;
    }

    @PostMapping("/{gameType}/{localeId}/{gameInstanceId}")
    public ResponseEntity<?> toggleAutoplay(
            @PathVariable String gameType,
            @PathVariable String localeId,
            @PathVariable String gameInstanceId,
            @RequestParam boolean enabled) {
        autoplayService.toggleAutoplay(gameType, localeId, gameInstanceId, enabled);
        return ResponseEntity.ok(enabled);
    }
    
    @GetMapping("/{gameInstanceId}")
    public ResponseEntity<Boolean> getAutoplayStatus(@PathVariable String gameInstanceId) {
        return ResponseEntity.ok(autoplayService.isAutoplayEnabled(gameInstanceId));
    }
}
