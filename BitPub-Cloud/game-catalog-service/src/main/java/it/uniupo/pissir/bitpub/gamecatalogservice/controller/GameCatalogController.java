package it.uniupo.pissir.bitpub.gamecatalogservice.controller;

import it.uniupo.pissir.bitpub.gamecatalogservice.dto.AddSensorRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.CreateGameTypeRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.GameTypeDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.SensorDefinitionDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.service.GameCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class GameCatalogController {

    private final GameCatalogService gameCatalogService;

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
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

    @PostMapping("/games/{gameTypeId}/sensors")
    @ResponseStatus(HttpStatus.CREATED)
    public SensorDefinitionDto addSensor(@PathVariable String gameTypeId, @Valid @RequestBody AddSensorRequest request) {
        return gameCatalogService.addSensorToGameType(gameTypeId, request);
    }

    @GetMapping("/games/{gameTypeId}/sensors")
    public List<SensorDefinitionDto> getSensorsByGameType(@PathVariable String gameTypeId) {
        return gameCatalogService.getSensorsByGameType(gameTypeId);
    }
}
