package com.bitpub.services;

import com.bitpub.models.EdgeStatus;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AdminNetworkService {
    private final RestClient restClient;

    public AdminNetworkService(RestClient restClient) {
        this.restClient = restClient;
    }

    public CompletableFuture<List<EdgeStatus>> getNetworkStatus() {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String networkUrl = root.getLinkSafe("network-status");
                return restClient.getAsync(networkUrl, EdgeStatus[].class);
            })
            .thenApply(statusArray -> statusArray != null ? Arrays.asList(statusArray) : List.of());
    }
}
