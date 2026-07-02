package it.uniupo.pissir.bitpub.localeservice.controller;

import it.uniupo.pissir.bitpub.localeservice.dto.AddGameInstanceRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.CreateLocaleRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.GameInstanceDto;
import it.uniupo.pissir.bitpub.localeservice.dto.LocaleDto;
import it.uniupo.pissir.bitpub.localeservice.service.LocaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locales")
@RequiredArgsConstructor
public class LocaleController {

    private final LocaleService localeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocaleDto createLocale(@Valid @RequestBody CreateLocaleRequest request) {
        return localeService.createLocale(request);
    }

    @GetMapping
    public List<LocaleDto> getAllLocales() {
        return localeService.getAllLocales();
    }

    @GetMapping("/{id}")
    public LocaleDto getLocaleById(@PathVariable String id) {
        return localeService.getLocaleById(id);
    }

    @GetMapping("/by-admin/{adminId}")
    public List<LocaleDto> getLocalesByAdmin(@PathVariable String adminId) {
        return localeService.getLocalesByAdmin(adminId);
    }

    // Gestione dei dispositivi: riservata al LOCALE_ADMIN proprietario del locale (o PLATFORM_ADMIN).
    // Ruolo e id del chiamante arrivano dal gateway (JwtAuthenticationFilter) negli header X-User-Id / X-User-Role.
    @PostMapping("/{localeId}/games")
    @ResponseStatus(HttpStatus.CREATED)
    public GameInstanceDto addGameInstance(@PathVariable String localeId, @Valid @RequestBody AddGameInstanceRequest request,
                                            @RequestHeader(value = "X-User-Id", required = false) String callerId,
                                            @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        return localeService.addGameInstance(localeId, request, callerId, callerRole);
    }

    @GetMapping("/{localeId}/games")
    public List<GameInstanceDto> getGameInstances(@PathVariable String localeId) {
        return localeService.getGameInstancesByLocale(localeId);
    }

    @GetMapping("/games/{gameInstanceId}")
    public GameInstanceDto getGameInstanceById(@PathVariable String gameInstanceId) {
        return localeService.getGameInstanceById(gameInstanceId);
    }

    @PatchMapping("/{localeId}/games/{gameInstanceId}/status")
    public GameInstanceDto setGameInstanceStatus(@PathVariable String localeId, @PathVariable String gameInstanceId, @RequestParam boolean active,
                                                  @RequestHeader(value = "X-User-Id", required = false) String callerId,
                                                  @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        return localeService.setGameInstanceActive(localeId, gameInstanceId, active, callerId, callerRole);
    }
}
