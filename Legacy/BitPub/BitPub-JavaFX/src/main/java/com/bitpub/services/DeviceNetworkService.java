package com.bitpub.services;

import com.bitpub.network.RestClient;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

public class DeviceNetworkService {
    private final RestClient restClient = RestClient.getInstance();

    public CompletableFuture<String> getDevicesByLocale(UUID localeId) {
        return restClient.getAsync(restClient.getRootUrl() + "/api/v1/devices/locale/" + localeId.toString(), String.class);
    }
    
    public CompletableFuture<String> registerDevice(String macAddress, UUID gameId, UUID localeId) {
        Map<String, Object> payload = Map.of(
            "macAddress", macAddress,
            "gameId", gameId,
            "localeId", localeId
        );
        return restClient.postAsync(restClient.getRootUrl() + "/api/v1/devices", payload, String.class);
    }

    public CompletableFuture<String> updateDeviceStatus(String deviceId, String status) {
        Map<String, Object> payload = Map.of("status", status);
        return restClient.putAsync(restClient.getRootUrl() + "/api/v1/devices/" + deviceId + "/status", payload, String.class);
    }
}
