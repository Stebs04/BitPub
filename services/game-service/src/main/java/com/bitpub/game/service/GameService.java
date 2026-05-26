package com.bitpub.game.service;

import com.bitpub.game.model.Device;
import com.bitpub.game.model.Game;
import com.bitpub.game.model.Locale;
import com.bitpub.game.repository.DeviceRepository;
import com.bitpub.game.repository.GameRepository;
import com.bitpub.game.repository.LocaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bitpub.common.dto.PageResponse;
import com.bitpub.common.specification.SearchCriteria;
import com.bitpub.game.specification.GameSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final LocaleRepository localeRepository;
    private final DeviceRepository deviceRepository;

    public PageResponse<Game> getGames(List<SearchCriteria> criteria, Pageable pageable) {
        Specification<Game> spec = GameSpecification.createSpecification(criteria);
        Page<Game> page = gameRepository.findAll(spec, pageable);
        return PageResponse.of(page);
    }

    @Transactional
    public Game createGame(Game game) {
        return gameRepository.save(game);
    }

    public List<Locale> getAllLocales() {
        return localeRepository.findAll();
    }

    @Transactional
    public Locale createLocale(Locale locale) {
        return localeRepository.save(locale);
    }

    public List<Device> getDevicesByLocale(UUID localeId) {
        return deviceRepository.findByLocaleId(localeId);
    }

    @Transactional
    public Device registerDevice(UUID localeId, UUID gameId, String macAddress) {
        Locale locale = localeRepository.findById(localeId).orElseThrow();
        Game game = gameRepository.findById(gameId).orElseThrow();

        Device device = Device.builder()
                .locale(locale)
                .game(game)
                .macAddress(macAddress)
                .status("OFFLINE")
                .build();
        return deviceRepository.save(device);
    }
}
