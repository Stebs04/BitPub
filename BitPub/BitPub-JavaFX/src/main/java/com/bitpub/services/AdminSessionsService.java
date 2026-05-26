package com.bitpub.services;

import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AdminSessionsService {
    private final RestClient restClient;

    public AdminSessionsService(RestClient restClient) {
        this.restClient = restClient;
    }

    public CompletableFuture<String> getEdgeStatus() {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String edgeStatusUrl = root.getLinks().get("edge-status").getHref();
                return restClient.getAsync(edgeStatusUrl, JsonObject.class);
            })
            .thenApply(json -> json.has("status") ? json.get("status").getAsString() : "UNKNOWN");
    }

    public CompletableFuture<List<JsonObject>> getActiveSessions() {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String activeSessionsUrl = root.getLinks().get("active-sessions").getHref();
                return restClient.getAsync(activeSessionsUrl, JsonObject.class);
            })
            .thenApply(rootObject -> {
                JsonArray jsonArray;
                if (rootObject.has("_embedded")) {
                    JsonObject embedded = rootObject.getAsJsonObject("_embedded");
                    String listKey = embedded.keySet().iterator().next(); 
                    jsonArray = embedded.getAsJsonArray(listKey);
                } else if (rootObject.has("content")) {
                    jsonArray = rootObject.getAsJsonArray("content");
                } else if (rootObject.isJsonArray()) {
                    jsonArray = rootObject.getAsJsonArray();
                } else {
                    jsonArray = new JsonArray();
                }

                List<JsonObject> sessions = new ArrayList<>();
                for (JsonElement el : jsonArray) {
                    sessions.add(el.getAsJsonObject());
                }
                return sessions;
            });
    }

    public CompletableFuture<Void> forceStopSession(String sessionId, String forceStopUrl) {
        if (forceStopUrl != null && !forceStopUrl.isEmpty()) {
            return restClient.postAsync(forceStopUrl, null, JsonObject.class).thenAccept(res -> {});
        } else {
            String rootUrl = restClient.getRootUrl(); 
            int apiIndex = rootUrl.indexOf("/api/v1");
            String baseUrl = (apiIndex != -1) ? rootUrl.substring(0, apiIndex) : "http://localhost:8080";
            String fallbackUrl = baseUrl + "/api/v1/admin/sessions/" + sessionId + "/force-stop";
            return restClient.postAsync(fallbackUrl, null, JsonObject.class).thenAccept(res -> {});
        }
    }
}
