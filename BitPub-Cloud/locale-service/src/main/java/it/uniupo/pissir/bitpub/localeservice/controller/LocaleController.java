package it.uniupo.pissir.bitpub.localeservice.controller;

import it.uniupo.pissir.bitpub.localeservice.dto.AddGameInstanceRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.CreateLocaleRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.GameInstanceDto;
import it.uniupo.pissir.bitpub.localeservice.dto.LocaleDto;
import it.uniupo.pissir.bitpub.localeservice.service.LocaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locales")
@RequiredArgsConstructor
public class LocaleController {

    private final LocaleService localeService;

    // Gestione locali (CRUD completo): creazione ed eliminazione riservate a PLATFORM_ADMIN;
    // la modifica e' consentita anche al LOCALE_ADMIN proprietario (verificato nel service).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public LocaleDto createLocale(@Valid @RequestBody CreateLocaleRequest request) {
        return localeService.createLocale(request);
    }

    @GetMapping
    public List<LocaleDto> getAllLocales() {
        return localeService.getAllLocales();
    }

    // Esplorazione PLAYER: locali attualmente ONLINE (con almeno una macchina simulata attiva),
    // calcolato in tempo reale sullo stato dei dispositivi, non una lista statica salvata a DB.
    @GetMapping("/online")
    public List<LocaleDto> getOnlineLocales() {
        return localeService.getOnlineLocales();
    }

    @GetMapping("/{id}")
    public LocaleDto getLocaleById(@PathVariable String id) {
        return localeService.getLocaleById(id);
    }

    @GetMapping("/by-admin/{adminId}")
    public List<LocaleDto> getLocalesByAdmin(@PathVariable String adminId) {
        return localeService.getLocalesByAdmin(adminId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'LOCALE_ADMIN')")
    public LocaleDto updateLocale(@PathVariable String id, @Valid @RequestBody CreateLocaleRequest request,
                                   @RequestHeader(value = "X-User-Id", required = false) String callerId,
                                   @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        return localeService.updateLocale(id, request, callerId, callerRole);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public void deleteLocale(@PathVariable String id) {
        localeService.deleteLocale(id);
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
