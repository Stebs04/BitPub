package it.uniupo.pissir.bitpub.simulators.controller;

import it.uniupo.pissir.bitpub.simulators.service.GenericSimulator;
import it.uniupo.pissir.bitpub.simulators.service.GenericSimulator.GameConfig;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * Espone i tipi di gioco che il pannello demo conosce: sono quelli pubblicati dal
 * game-catalog-service su bitpub/config/games/# e tenuti in cache dal GenericSimulator.
 * Il frontend genera i joypad da questa lista — nessun gioco hard-coded.
 */
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")
public class GamesController {

    private final GenericSimulator simulator;

    public GamesController(GenericSimulator simulator) {
        this.simulator = simulator;
    }

    @GetMapping
    public Collection<GameConfig> games() {
        return simulator.configs();
    }
}
