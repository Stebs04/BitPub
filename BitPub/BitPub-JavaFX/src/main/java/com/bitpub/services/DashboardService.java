package com.bitpub.services;

import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import com.bitpub.network.SessionContext;
import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

/**
 * Service per la gestione delle operazioni della Dashboard Utente.
 */
public class DashboardService {

    private final RestClient restClient;

    public DashboardService() {
        this.restClient = RestClient.getInstance();
    }

    /**
     * Recupera asincronamente il profilo utente corrente.
     */
    public CompletableFuture<JsonObject> getUserProfileAsync() {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String userUrl = root.getLinkSafe("me");
                return restClient.getAsync(userUrl, JsonObject.class);
            });
    }

    /**
     * Avvia una sessione di Calciobalilla.
     */
    public CompletableFuture<Void> startFoosballSessionAsync(int tableId) {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String startUrl = root.getLinkSafe("foosball-start");
                JsonObject payload = new JsonObject();
                payload.addProperty("table_id", tableId);
                return restClient.postAsync(startUrl, payload, JsonObject.class);
            })
            .thenAccept(this::salvaInfoSessione);
    }

    /**
     * Tenta di recuperare una sessione attiva in caso di conflitto.
     */
    public CompletableFuture<Void> recoverActiveFoosballSessionAsync() {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String currentUrl = root.getLinkSafe("foosball-current");
                return restClient.getAsync(currentUrl, JsonObject.class);
            })
            .thenAccept(this::salvaInfoSessione);
    }

    private void salvaInfoSessione(JsonObject session) {
        long sessionId = session.get("id").getAsLong();
        SessionContext.setCurrentSessionId(sessionId);

        if (session.has("_links")) {
            JsonObject links = session.getAsJsonObject("_links");
            if (links.has("self")) {
                String statusUrl = links.getAsJsonObject("self").get("href").getAsString();
                SessionContext.setCurrentSessionStatusUrl(statusUrl);
            }
        }
    }
}
