package com.bitpub.services;

import com.bitpub.network.RestClient;
import java.util.concurrent.CompletableFuture;

public class StatsNetworkService {
    private final RestClient restClient = RestClient.getInstance();

    public CompletableFuture<String> getStats() {
        return restClient.getAsync(restClient.getRootUrl() + "/api/v1/stats", String.class);
    }
    
    public CompletableFuture<String> getGlobalLeaderboard() {
        return restClient.getAsync(restClient.getRootUrl() + "/api/v1/stats/leaderboard/global", String.class);
    }
    
    public CompletableFuture<String> getMatchHistory(java.util.UUID userId) {
        return restClient.getAsync(restClient.getRootUrl() + "/api/v1/stats/history/" + userId.toString(), String.class);
    }
}
