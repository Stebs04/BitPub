package com.bitpub.services;

import com.bitpub.javafx.model.LeaderboardEntryModel;
import com.bitpub.network.RestClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StatsService {
    private final RestClient restClient;

    public StatsService() {
        this.restClient = RestClient.getInstance();
    }

    public CompletableFuture<List<LeaderboardEntryModel>> getGlobalLeaderboard(int page) {
        String endpoint = "/api/v1/stats/leaderboard/global?page=" + page + "&size=20";
        // Convert to absolute URL using rootUrl base
        String rootUrl = restClient.getRootUrl();
        int apiIndex = rootUrl.indexOf("/api/v1");
        String baseUrl = (apiIndex != -1) ? rootUrl.substring(0, apiIndex) : "http://localhost:8080";
        String fullUrl = baseUrl + endpoint;

        return restClient.getAsync(fullUrl, JsonObject.class)
            .thenApply(this::parseLeaderboardResponse);
    }

    public CompletableFuture<List<LeaderboardEntryModel>> getGameLeaderboard(String gameId, int page) {
        String endpoint = "/api/v1/stats/leaderboard/game/" + gameId + "?page=" + page + "&size=20";
        String rootUrl = restClient.getRootUrl();
        int apiIndex = rootUrl.indexOf("/api/v1");
        String baseUrl = (apiIndex != -1) ? rootUrl.substring(0, apiIndex) : "http://localhost:8080";
        String fullUrl = baseUrl + endpoint;

        return restClient.getAsync(fullUrl, JsonObject.class)
            .thenApply(this::parseLeaderboardResponse);
    }

    private List<LeaderboardEntryModel> parseLeaderboardResponse(JsonObject response) {
        List<LeaderboardEntryModel> list = new ArrayList<>();
        JsonArray array = null;
        if (response.has("content")) {
            array = response.getAsJsonArray("content");
        } else if (response.has("_embedded")) {
            JsonObject embedded = response.getAsJsonObject("_embedded");
            String key = embedded.keySet().iterator().next();
            array = embedded.getAsJsonArray(key);
        } else if (response.isJsonArray()) {
            array = response.getAsJsonArray();
        }

        if (array != null) {
            for (JsonElement el : array) {
                list.add(restClient.getGson().fromJson(el, LeaderboardEntryModel.class));
            }
        }
        return list;
    }
}
