package com.bitpub.services;

import com.bitpub.models.Utente;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AdminUsersService {
    private final RestClient restClient;

    public AdminUsersService() {
        this.restClient = RestClient.getInstance();
    }

    public CompletableFuture<List<Utente>> getUsers(String query) {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String usersUrl = root.getLinkSafe("users");
                if (query != null && !query.trim().isEmpty()) {
                    usersUrl += "?search=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
                }
                return restClient.getAsync(usersUrl, JsonObject.class);
            })
            .thenApply(this::extractUsersFromHateoas);
    }

    public CompletableFuture<Void> toggleRole(String toggleUrl) {
        return restClient.putAsync(toggleUrl, null, JsonObject.class).thenAccept(res -> {});
    }

    public CompletableFuture<Void> toggleStatus(String username) {
        String usernameEncoded = URLEncoder.encode(username, StandardCharsets.UTF_8).replace("+", "%20");
        String endpoint = restClient.getRootUrl().replace("/home", "") + "/users/" + usernameEncoded + "/toggle-status";
        return restClient.putAsync(endpoint, null, JsonObject.class).thenAccept(res -> {});
    }

    private List<Utente> extractUsersFromHateoas(JsonObject response) {
        List<Utente> utenti = new ArrayList<>();
        try {
            if (response.has("_embedded")) {
                JsonObject embedded = response.getAsJsonObject("_embedded");
                String key = embedded.keySet().iterator().next();
                JsonArray array = embedded.getAsJsonArray(key);
                for (JsonElement el : array) {
                    utenti.add(restClient.getGson().fromJson(el, Utente.class));
                }
            } else if (response.has("content")) {
                JsonArray array = response.getAsJsonArray("content");
                for (JsonElement el : array) {
                    utenti.add(restClient.getGson().fromJson(el, Utente.class));
                }
            }
        } catch (Exception e) {
            System.err.println("Errore parsing HATEOAS utenti: " + e.getMessage());
        }
        return utenti;
    }
}
