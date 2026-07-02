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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocaleService {

    private final LocaleRepository localeRepository;
    private final GameInstanceRepository gameInstanceRepository;

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
        return mapToDto(saved);
    }

    public LocaleDto getLocaleById(String id) {
        Locale locale = localeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Locale", "id", id));
        return mapToDto(locale);
    }

    public List<LocaleDto> getLocalesByAdmin(String adminId) {
        return localeRepository.findByAdminId(adminId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<LocaleDto> getAllLocales() {
        return localeRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GameInstanceDto addGameInstance(String localeId, AddGameInstanceRequest request) {
        Locale locale = localeRepository.findById(localeId)
                .orElseThrow(() -> new ResourceNotFoundException("Locale", "id", localeId));

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

    public List<GameInstanceDto> getGameInstancesByLocale(String localeId) {
        return gameInstanceRepository.findByLocaleId(localeId).stream()
                .map(this::mapGameInstanceToDto)
                .collect(Collectors.toList());
    }

    /**
     * Toggles a simulated device on/off for the LOCALE_ADMIN device-config panel.
     */
    @Transactional
    public GameInstanceDto setGameInstanceActive(String localeId, String gameInstanceId, boolean active) {
        GameInstance instance = gameInstanceRepository.findById(gameInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("GameInstance", "id", gameInstanceId));

        if (!instance.getLocale().getId().equals(localeId)) {
            throw new ResourceNotFoundException("GameInstance", "id", gameInstanceId);
        }

        instance.setActive(active);
        return mapGameInstanceToDto(gameInstanceRepository.save(instance));
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
                .installedAt(instance.getInstalledAt())
                .active(instance.isActive())
                .build();
    }
}
