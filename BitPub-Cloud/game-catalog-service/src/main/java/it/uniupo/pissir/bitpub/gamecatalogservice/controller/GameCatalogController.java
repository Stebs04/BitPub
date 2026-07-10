/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice.controller;

import it.uniupo.pissir.bitpub.gamecatalogservice.dto.AddSensorRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.CreateGameTypeRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.GameTypeDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.SensorDefinitionDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.service.GameCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class GameCatalogController {

    private final GameCatalogService gameCatalogService;

    // Le operazioni di scrittura sul catalogo (tipi di gioco, configurazione sensori) sono esclusive del ruolo GAME_ADMIN.
    // Al contrario, l'accesso in sola lettura è consentito globalmente per permettere agli amministratori locali la consultazione del parco giochi.
    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GAME_ADMIN')")
    public GameTypeDto createGameType(@Valid @RequestBody CreateGameTypeRequest request) {
        return gameCatalogService.createGameType(request);
    }

    @GetMapping("/games")
    public List<GameTypeDto> getAllGameTypes() {
        return gameCatalogService.getAllGameTypes();
    }

    @GetMapping("/games/{id}")
    public GameTypeDto getGameTypeById(@PathVariable String id) {
        return gameCatalogService.getGameTypeById(id);
    }

    @PutMapping("/games/{id}")
    @PreAuthorize("hasRole('GAME_ADMIN')")
    public GameTypeDto updateGameType(@PathVariable String id, @Valid @RequestBody CreateGameTypeRequest request) {
        return gameCatalogService.updateGameType(id, request);
    }

    @DeleteMapping("/games/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GAME_ADMIN')")
    public void deleteGameType(@PathVariable String id) {
        gameCatalogService.deleteGameType(id);
    }

    @DeleteMapping("/games/{gameTypeId}/sensors/{sensorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GAME_ADMIN')")
    public void deleteSensor(@PathVariable String gameTypeId, @PathVariable String sensorId) {
        gameCatalogService.deleteSensor(gameTypeId, sensorId);
    }

    @PostMapping("/games/{gameTypeId}/sensors")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GAME_ADMIN')")
    public SensorDefinitionDto addSensor(@PathVariable String gameTypeId, @Valid @RequestBody AddSensorRequest request) {
        return gameCatalogService.addSensorToGameType(gameTypeId, request);
    }

    @GetMapping("/games/{gameTypeId}/sensors")
    public List<SensorDefinitionDto> getSensorsByGameType(@PathVariable String gameTypeId) {
        return gameCatalogService.getSensorsByGameType(gameTypeId);
    }
}
