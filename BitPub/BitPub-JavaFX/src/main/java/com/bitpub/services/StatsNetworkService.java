package com.bitpub.services;

import com.bitpub.network.RestClient;
import java.util.concurrent.CompletableFuture;

public class StatsNetworkService {
    private final RestClient restClient = RestClient.getInstance();

    public CompletableFuture<String> getStats() {
        return restClient.getAsync(restClient.getRootUrl() + "/api/v1/stats", String.class);
    }
}
