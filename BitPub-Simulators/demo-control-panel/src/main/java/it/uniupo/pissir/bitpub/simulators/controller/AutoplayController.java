/**
 * autore Timothy Giolito 20054431
 *
 * Controller REST incaricato di gestire lo stato dell'autoplay.
 * Consente l'attivazione o disattivazione della modalità di gioco automatico
 * per specifiche istanze e ne permette l'interrogazione dello stato attuale.
 */
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
            @PathVariable("gameType") String gameType,
            @PathVariable("localeId") String localeId,
            @PathVariable("gameInstanceId") String gameInstanceId,
            @RequestParam("enabled") boolean enabled) {
        autoplayService.toggleAutoplay(gameType, localeId, gameInstanceId, enabled);
        return ResponseEntity.ok(enabled);
    }
    
    @GetMapping("/{gameInstanceId}")
    public ResponseEntity<Boolean> getAutoplayStatus(@PathVariable("gameInstanceId") String gameInstanceId) {
        return ResponseEntity.ok(autoplayService.isAutoplayEnabled(gameInstanceId));
    }
}
