/**
 * autore Timothy Giolito 20054431
 *
 * Controller che si occupa di esporre le configurazioni relative ai giochi disponibili.
 * Viene interrogato per ottenere l'elenco dei giochi che il simulatore conosce, 
 * in modo da permettere la costruzione dinamica dell'interfaccia.
 */
package it.uniupo.pissir.bitpub.simulators.controller;

import it.uniupo.pissir.bitpub.simulators.service.GenericSimulator;
import it.uniupo.pissir.bitpub.simulators.service.GenericSimulator.GameConfig;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

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
