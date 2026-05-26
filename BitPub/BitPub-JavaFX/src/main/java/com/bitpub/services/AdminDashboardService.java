package com.bitpub.services;

import com.bitpub.models.Locale;
import com.bitpub.models.Utente;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import com.bitpub.network.HateoasUtils;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AdminDashboardService {
    private final RestClient restClient;

    public AdminDashboardService(RestClient restClient) {
        this.restClient = restClient;
    }

    public CompletableFuture<List<Locale>> getLocali() {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String linkLocali = root.getLinkSafe("locali");
                return restClient.getAsync(linkLocali, JsonObject.class);
            })
            .thenApply(response -> HateoasUtils.extractArrayFromHateoas(response, Locale.class, RestClient.getInstance().getGson()));
    }

    public CompletableFuture<List<Utente>> getGestori() {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String usersUrl = root.getLinkSafe("users") + "?role=GESTORE";
                return restClient.getAsync(usersUrl, JsonObject.class);
            })
            .thenApply(response -> HateoasUtils.extractArrayFromHateoas(response, Utente.class, RestClient.getInstance().getGson()));
    }

    public CompletableFuture<Locale> createLocale(Locale nuovoLocale) {
        return restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String createUrl = root.getLinkSafe("locali");
                return restClient.postAsync(createUrl, nuovoLocale, Locale.class);
            });
    }

    public CompletableFuture<Locale> updateLocale(String updateUrl, Locale locale) {
        return restClient.putAsync(updateUrl, locale, Locale.class);
    }

    public CompletableFuture<Void> deleteLocale(String deleteUrl) {
        return restClient.deleteAsync(deleteUrl);
    }
}
