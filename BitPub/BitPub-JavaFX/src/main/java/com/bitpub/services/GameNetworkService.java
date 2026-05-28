package com.bitpub.services;

import com.bitpub.network.RestClient;
import java.util.concurrent.CompletableFuture;

public class GameNetworkService {
    private final RestClient restClient = RestClient.getInstance();

    public CompletableFuture<String> getGames() {
        return restClient.getAsync(restClient.getRootUrl() + "/api/v1/games", String.class);
    }
    
    public CompletableFuture<String> createGame(com.bitpub.model.Game game) {
        return restClient.postAsync(restClient.getRootUrl() + "/api/v1/games", game, String.class);
    }
}
