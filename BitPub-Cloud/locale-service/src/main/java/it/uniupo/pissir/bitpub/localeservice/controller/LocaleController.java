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

/**
 * Autore: Stefano Bellan Matricola 20054330
 * 
 * Controller per esporre le API REST relative alla gestione dei locali e delle relative macchine da gioco.
 */
@RestController
@RequestMapping("/api/v1/locales")
@RequiredArgsConstructor
public class LocaleController {

    private final LocaleService localeService;

    // Gestione dei locali: creazione ed eliminazione sono operazioni riservate agli amministratori di piattaforma.
    // L'aggiornamento e' consentito anche all'amministratore del locale proprietario.
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

    // Restituisce la lista dei locali attualmente in attivita', calcolata verificando 
    // la presenza di almeno un dispositivo simulato operativo.
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

    // Gestione dei dispositivi di gioco: operazioni limitate agli amministratori del locale.
    // Il ruolo e l'identificativo del chiamante vengono estratti dagli header inviati dal gateway.
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

    @DeleteMapping("/{localeId}/games/{gameInstanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeGameInstance(@PathVariable String localeId, @PathVariable String gameInstanceId,
                                    @RequestHeader(value = "X-User-Id", required = false) String callerId,
                                    @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        localeService.removeGameInstance(localeId, gameInstanceId, callerId, callerRole);
    }

    @PatchMapping("/{localeId}/games/{gameInstanceId}/status")
    public GameInstanceDto setGameInstanceStatus(@PathVariable String localeId, @PathVariable String gameInstanceId, @RequestParam boolean active,
                                                  @RequestHeader(value = "X-User-Id", required = false) String callerId,
                                                  @RequestHeader(value = "X-User-Role", required = false) String callerRole) {
        return localeService.setGameInstanceActive(localeId, gameInstanceId, active, callerId, callerRole);
    }
}
