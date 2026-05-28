package com.bitpub.services;

import com.bitpub.network.RestClient;
import java.util.concurrent.CompletableFuture;

public class TournamentNetworkService {
    private final RestClient restClient = RestClient.getInstance();

    public CompletableFuture<String> getTournaments() {
        return restClient.getAsync(restClient.getRootUrl() + "/api/v1/tournaments", String.class);
    }
}
