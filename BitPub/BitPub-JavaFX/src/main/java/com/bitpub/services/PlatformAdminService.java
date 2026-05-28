package com.bitpub.services;

import com.bitpub.network.RestClient;
import java.util.concurrent.CompletableFuture;

public class PlatformAdminService {
    private final RestClient restClient = RestClient.getInstance();

    public CompletableFuture<String> getUsers() {
        return restClient.getAsync(restClient.getRootUrl() + "/api/v1/auth/users", String.class);
    }
}
