package com.bitpub.services;

import com.bitpub.models.SystemLog;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AdminLogsService {
    private final RestClient restClient;

    public AdminLogsService(RestClient restClient) {
        this.restClient = restClient;
    }

    public CompletableFuture<List<SystemLog>> getLogs(String level) {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String logsUrl = root.getLinkSafe("system-logs");
                if (level != null && !"ALL".equals(level)) {
                    logsUrl += "?level=" + level;
                }
                return restClient.getAsync(logsUrl, SystemLog[].class);
            })
            .thenApply(logArray -> logArray != null ? Arrays.asList(logArray) : List.of());
    }
}
