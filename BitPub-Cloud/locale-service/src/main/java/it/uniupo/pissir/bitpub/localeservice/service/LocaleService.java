package it.uniupo.pissir.bitpub.localeservice.service;

import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.exception.ResourceNotFoundException;
import it.uniupo.pissir.bitpub.localeservice.domain.GameInstance;
import it.uniupo.pissir.bitpub.localeservice.domain.Locale;
import it.uniupo.pissir.bitpub.localeservice.dto.AddGameInstanceRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.CreateLocaleRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.GameInstanceDto;
import it.uniupo.pissir.bitpub.localeservice.dto.LocaleDto;
import it.uniupo.pissir.bitpub.localeservice.repository.GameInstanceRepository;
import it.uniupo.pissir.bitpub.localeservice.repository.LocaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocaleService {

    private final LocaleRepository localeRepository;
    private final GameInstanceRepository gameInstanceRepository;

    @Value("${user.service.url:http://localhost:8082}")
    private String userServiceUrl;

    @Transactional
    public LocaleDto createLocale(CreateLocaleRequest request) {
        Locale locale = Locale.builder()
                .name(request.getName())
                .address(request.getAddress())
                .adminId(request.getAdminId())
                .createdAt(Instant.now())
                .gameInstances(new ArrayList<>())
                .build();

        Locale saved = localeRepository.save(locale);
        syncAdminLocaleId(saved.getAdminId(), saved.getId());
        return mapToDto(saved);
    }

    /**
     * Sincronizza il localeId sull'utente LOCALE_ADMIN, cosi' che al login successivo
     * il claim JWT (e X-User-Locale-Id inoltrato dal gateway) rifletta il locale assegnato.
     */
    private void syncAdminLocaleId(String adminId, String localeId) {
        if (adminId == null) return;
        try {
            RestClient.create(userServiceUrl)
                    .patch()
                    .uri("/api/v1/users/{id}/locale?localeId={localeId}", adminId, localeId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to sync localeId for adminId: {}", adminId, e);
        }
    }

    @Transactional(readOnly = true)
    public LocaleDto getLocaleById(String id) {
        Locale locale = localeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Locale", "id", id));
        return mapToDto(locale);
    }

    /**
     * Modifica di un locale: consentita al PLATFORM_ADMIN o al LOCALE_ADMIN proprietario del locale.
     */
    @Transactional
    public LocaleDto updateLocale(String id, CreateLocaleRequest request, String callerId, String callerRole) {
        Locale locale = localeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Locale", "id", id));

        assertLocaleManageable(locale, callerId, callerRole);

        locale.setName(request.getName());
        locale.setAddress(request.getAddress());
        locale.setAdminId(request.getAdminId());

        Locale saved = localeRepository.save(locale);
        syncAdminLocaleId(saved.getAdminId(), saved.getId());
        return mapToDto(saved);
    }

    /**
     * Eliminazione di un locale: operazione riservata al PLATFORM_ADMIN (CRUD completo sui locali).
     */
    @Transactional
    public void deleteLocale(String id) {
        if (!localeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Locale", "id", id);
        }
        localeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<LocaleDto> getLocalesByAdmin(String adminId) {
        return localeRepository.findByAdminId(adminId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocaleDto> getAllLocales() {
        return localeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GameInstanceDto addGameInstance(String localeId, AddGameInstanceRequest request, String callerId, String callerRole) {
        Locale locale = localeRepository.findById(localeId)
                .orElseThrow(() -> new ResourceNotFoundException("Locale", "id", localeId));

        assertLocaleManageable(locale, callerId, callerRole);

        if (gameInstanceRepository.findByLocaleIdAndLocalInstanceId(localeId, request.getLocalInstanceId()).isPresent()) {
            throw new BitpubException("A game instance with the same localInstanceId already exists in this locale", HttpStatus.CONFLICT);
        }

        GameInstance instance = GameInstance.builder()
                .localInstanceId(request.getLocalInstanceId())
                .gameTypeId(request.getGameTypeId())
                .locale(locale)
                .installedAt(Instant.now())
                .active(true)
                .build();

        GameInstance saved = gameInstanceRepository.save(instance);
        return mapGameInstanceToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<GameInstanceDto> getGameInstancesByLocale(String localeId) {
        return gameInstanceRepository.findByLocaleId(localeId).stream()
                .map(this::mapGameInstanceToDto)
                .collect(Collectors.toList());
    }

    /**
     * Toggles a simulated device on/off for the LOCALE_ADMIN device-config panel.
     */
    @Transactional
    public GameInstanceDto setGameInstanceActive(String localeId, String gameInstanceId, boolean active, String callerId, String callerRole) {
        GameInstance instance = gameInstanceRepository.findById(gameInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("GameInstance", "id", gameInstanceId));

        if (!instance.getLocale().getId().equals(localeId)) {
            throw new ResourceNotFoundException("GameInstance", "id", gameInstanceId);
        }

        assertLocaleManageable(instance.getLocale(), callerId, callerRole);

        instance.setActive(active);
        return mapGameInstanceToDto(gameInstanceRepository.save(instance));
    }

    @Transactional(readOnly = true)
    public GameInstanceDto getGameInstanceById(String gameInstanceId) {
        GameInstance instance = gameInstanceRepository.findById(gameInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("GameInstance", "id", gameInstanceId));
        return mapGameInstanceToDto(instance);
    }

    /**
     * A LOCALE_ADMIN may only manage game instances of the locale they are assigned to (locale.adminId).
     * PLATFORM_ADMIN bypasses the check.
     */
    private void assertLocaleManageable(Locale locale, String callerId, String callerRole) {
        if ("PLATFORM_ADMIN".equals(callerRole)) {
            return;
        }
        if ("LOCALE_ADMIN".equals(callerRole)) {
            if (callerId == null || !callerId.equals(locale.getAdminId())) {
                throw new BitpubException("LOCALE_ADMIN can only manage their own locale", HttpStatus.FORBIDDEN);
            }
            return;
        }
        throw new BitpubException("Only LOCALE_ADMIN or PLATFORM_ADMIN can manage locale devices", HttpStatus.FORBIDDEN);
    }

    private LocaleDto mapToDto(Locale locale) {
        return LocaleDto.builder()
                .id(locale.getId())
                .name(locale.getName())
                .address(locale.getAddress())
                .adminId(locale.getAdminId())
                .games(locale.getGameInstances() != null ? 
                        locale.getGameInstances().stream().map(this::mapGameInstanceToDto).collect(Collectors.toList()) : 
                        new ArrayList<>())
                .build();
    }

    private GameInstanceDto mapGameInstanceToDto(GameInstance instance) {
        return GameInstanceDto.builder()
                .id(instance.getId())
                .localInstanceId(instance.getLocalInstanceId())
                .gameTypeId(instance.getGameTypeId())
                .localeId(instance.getLocale() != null ? instance.getLocale().getId() : null)
                .installedAt(instance.getInstalledAt())
                .active(instance.isActive())
                .build();
    }
}
