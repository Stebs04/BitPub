package com.bitpub.network;

import com.bitpub.Main;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service Layer per la gestione asincrona delle comunicazioni HTTP verso il Cloud BitPub.
 * Totalmente basato su HATEOAS, java.net.http.HttpClient e CompletableFuture.
 * Centralizza la gestione degli errori e l'intercettazione dei token scaduti.
 * @author Stefano Bellan 20054330
 */
public class RestClient {

    private static volatile RestClient instance;
    private final HttpClient client;
    private final Gson gson;

    // Legge l'URL dalle variabili d'ambiente (es. per Docker/Script) o usa il localhost come default
    private static final String ROOT_URL = System.getenv("BITPUB_CLOUD_URL") !=  null
                                           ? System.getenv("BITPUB_CLOUD_URL")
                                           : "http://localhost:8080";

    private static final String ACCEPT_HEADER = "application/json";

    private RestClient() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = com.bitpub.utils.JsonManager.getGson();
    }

    public static RestClient getInstance() {
        if (instance == null) {
            synchronized (RestClient.class) {
                if (instance == null) {
                    instance = new RestClient();
                }
            }
        }
        return instance;
    }

    /**
     * Punto di partenza per la navigazione HATEOAS.
     */
    public String getRootUrl() {
        return ROOT_URL;
    }

    public Gson getGson() {
        return gson;
    }

    // =========================================================================
    // METODI CORE SINCRONI (LEGACY SUPPORT)
    // =========================================================================

    public String get(String url) {
        try {
            return getAsync(url, String.class).join();
        } catch (Exception e) {
            throw new ApiException("Errore durante get sincrona: " + e.getMessage());
        }
    }

    public String post(String url, String payload) {
        try {
            // postAsync usa gson.toJson se non è una stringa, ma se gli passiamo
            // una String che è già JSON, gson.toJson la incapsula come stringa JSON.
            // Per supportare il vecchio post(String, String) che passava JSON grezzo:
            HttpRequest request = buildRequest(url)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload != null ? payload : "{}"))
                    .build();
            return executeRequest(request, String.class).join();
        } catch (Exception e) {
            throw new ApiException("Errore durante post sincrona: " + e.getMessage());
        }
    }

    public String put(String url, String payload) {
        try {
            HttpRequest request = buildRequest(url)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(payload != null ? payload : "{}"))
                    .build();
            return executeRequest(request, String.class).join();
        } catch (Exception e) {
            throw new ApiException("Errore durante put sincrona: " + e.getMessage());
        }
    }

    public String delete(String url) {
        try {
            deleteAsync(url).join();
            return "";
        } catch (Exception e) {
            throw new ApiException("Errore durante delete sincrona: " + e.getMessage());
        }
    }

    // =========================================================================
    // METODI CORE ASINCRONI
    // =========================================================================

    public <T> CompletableFuture<T> getAsync(String url, Class<T> responseClass) {
        HttpRequest request = buildRequest(url).GET().build();
        return executeRequest(request, responseClass);
    }

    public <T> CompletableFuture<T> postAsync(String url, Object payload, Class<T> responseClass) {
        String jsonPayload = (payload != null) ? gson.toJson(payload) : "{}";
        HttpRequest request = buildRequest(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        return executeRequest(request, responseClass);
    }

    public <T> CompletableFuture<T> putAsync(String url, Object payload, Class<T> responseClass) {
        String jsonPayload = (payload != null) ? gson.toJson(payload) : "{}";
        HttpRequest request = buildRequest(url)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        return executeRequest(request, responseClass);
    }

    public CompletableFuture<Void> deleteAsync(String url) {
        HttpRequest request = buildRequest(url).DELETE().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::checkErrors)
                .thenAccept(body -> {}); // Consuma la risposta senza restituire dati
    }

    // =========================================================================
    // METODI DI SUPPORTO E GESTIONE ERRORI
    // =========================================================================

    /**
     * Costruisce la base della richiesta HTTP iniettando Header HATEOAS e JWT Token.
     */
    private HttpRequest.Builder buildRequest(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", ACCEPT_HEADER)
                .timeout(Duration.ofSeconds(10));

        String token = SessionManager.getInstance().getJwtToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    /**
     * Esegue la richiesta, controlla gli errori di stato HTTP ed effettua il parsing JSON.
     */
    private <T> CompletableFuture<T> executeRequest(HttpRequest request, Class<T> responseClass) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::checkErrors) // <-- Intercettatore
                .thenApply(body -> {
                    // Se ci si aspetta solo una stringa pura, evita GSON
                    if (responseClass == String.class) {
                        return responseClass.cast(body);
                    }
                    try {
                        return gson.fromJson(body, responseClass);
                    } catch (JsonSyntaxException e) {
                        throw new HttpParsingException("Errore di parsing JSON nel RestClient", e);
                    }
                });
    }

    /**
     * Verifica il codice di stato HTTP. Se rileva 401 o 403, avvia la procedura di sicurezza.
     */
    private String checkErrors(HttpResponse<String> response) {
        int status = response.statusCode();

        if (status == 401) {
            handleAuthError();
            throw new ApiException("Sessione scaduta (" + status + "): " + response.body());
        } else if (status == 403) {
            throw new ApiException("Accesso negato (" + status + "): Non hai i permessi per questa operazione.");
        } else if (status >= 500) {
            throw new ApiException("Errore interno del server (" + status + "): " + response.body());
        } else if (status >= 400) {
            throw new ApiException("Richiesta errata (" + status + "): " + response.body());
        }

        return response.body();
    }

    /**
     * Invalida la sessione in background e usa il JavaFX Application Thread 
     * per visualizzare l'errore e fare il redirect, garantendo la Thread Safety.
     */
    private void handleAuthError() {
        SessionManager.getInstance().logout();
        SessionContext.clearAll();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Sessione Scaduta");
            alert.setHeaderText("Accesso Negato");
            alert.setContentText("La tua sessione è scaduta o non hai i permessi necessari. Verrai reindirizzato al Login.");
            alert.showAndWait();
            
            Main.eseguiLogout();
        });
    }

    /**
     * Eccezione personalizzata per gli errori di rete HTTP.
     */
    public static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }
    }
}