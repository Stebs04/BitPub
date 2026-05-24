package com.bitpub.game.controller;

import com.bitpub.game.model.Device;
import com.bitpub.game.service.GameService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final GameService gameService;

    @GetMapping("/locale/{localeId}")
    public ResponseEntity<List<Device>> getDevicesByLocale(@PathVariable UUID localeId) {
        return ResponseEntity.ok(gameService.getDevicesByLocale(localeId));
    }

    @PostMapping
    public ResponseEntity<Device> registerDevice(@RequestBody DeviceRegistrationRequest request) {
        return ResponseEntity.ok(gameService.registerDevice(request.getLocaleId(), request.getGameId(), request.getMacAddress()));
    }

    @Data
    static class DeviceRegistrationRequest {
        private UUID localeId;
        private UUID gameId;
        private String macAddress;
    }
}
