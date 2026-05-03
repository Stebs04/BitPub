package com.bitpub.network;

import com.bitpub.utils.JsonManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service Layer per la gestione delle comunicazioni HTTP verso il Cloud BitPub.
 * Questa classe incapsula la logica di rete, gestendo la serializzazione JSON
 * e le chiamate asincrone per non bloccare il thread principale della UI.
 *
 * @author Stefano Bellan, Timothy Giolito, Luca Franzon
 */
public class RestClient {

    // --- LOGICA DI STEFANO: Core Engine (Singleton Pattern) ---
    private static volatile RestClient instance;
    private final HttpClient client;

    /** URL base dell'API di backend (Spring Boot) */
    private final String baseUrl = "http://localhost:8080";

    // --- LOGICA DI TIMOTHY: Versioning e Headers ---
    /** Header specifico per il Semantic Versioning richiesto dalla Fase 13 */
    private static final String ACCEPT_HEADER = "application/resources.v1+json";

    /**
     * Costruttore privato.
     * Stefano: Inizializza il client HTTP nativo ottimizzato per Java 11+.
     */
    private RestClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Recupera l'istanza unica del client (Thread-safe con Double-checked locking).
     */
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

    // =========================================================================
    // NUOVI METODI SINCRONI (Modifiche per le specifiche del MODULO 1)
    // =========================================================================

    /**
     * Invia una richiesta GET sincrona con timeout di 10 secondi.
     */
    public String sendGet(String endpoint) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Accept", ACCEPT_HEADER)
                .timeout(Duration.ofSeconds(10))
                .GET();

        return sendSyncRequest(builder);
    }

    /**
     * Invia una richiesta POST sincrona con body JSON e timeout di 10 secondi.
     */
    public String sendPost(String endpoint, String jsonPayload) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", ACCEPT_HEADER)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload != null ? jsonPayload : "{}"));

        return sendSyncRequest(builder);
    }

    /**
     * Metodo privato per inviare le richieste HTTP sincrone, iniettare il token e gestire gli errori 401 e 5xx.
     */
    private String sendSyncRequest(HttpRequest.Builder builder) throws Exception {
        // Assicurati che SessionContext sia stato implementato e accessibile
        String token = SessionManager.getInstance().getJwtToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            // Rilancia alert e pulisce il token
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Sessione Scaduta");
                alert.setHeaderText("Sessione scaduta");
                alert.setContentText("Sessione scaduta, effettua nuovamente il login.");
                alert.showAndWait();
            });
            SessionContext.clearAll();
            SessionManager.getInstance().logout();
            throw new ApiException("Sessione scaduta (401)");
            
        } else if (response.statusCode() >= 500) {
            System.err.println("Errore del Server (5xx): " + response.body());
            throw new ApiException("Errore del server: " + response.statusCode());
            
        } else if (response.statusCode() >= 400) {
            throw new RuntimeException("Errore HTTP Client: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }

    /**
     * Eccezione personalizzata per gli errori API.
     */
    public static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }
    }

    // =========================================================================
    // VECCHI METODI ASINCRONI (Lasciati invariati per retrocompatibilità)
    // =========================================================================

    public <T> CompletableFuture<T> faiChiamataGet(String endpoint, Class<T> responseClass) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Accept", ACCEPT_HEADER)
                .GET();

        String token = SessionManager.getInstance().getJwtToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Errore HTTP dal server: " + response.statusCode());
                    }
                    return JsonManager.getInstance().fromJson(response.body(), responseClass);
                });
    }

    public <T> CompletableFuture<T> faiChiamataPost(String endpoint, Object data, Class<T> responseClass) {
        String json = JsonManager.getInstance().toJson(data);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", ACCEPT_HEADER)
                .POST(HttpRequest.BodyPublishers.ofString(json));

        String token = SessionManager.getInstance().getJwtToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Errore HTTP dal server: " + response.statusCode() + " - " + response.body());
                    }
                    return JsonManager.getInstance().fromJson(response.body(), responseClass);
                });
    }

    public void postAsync(String endpoint, Object data, Consumer<String> callback) {
        sendAsyncWithCallback("POST", endpoint, data, callback);
    }

    public void deleteAsync(String endpoint, Consumer<String> callback) {
        sendAsyncWithCallback("DELETE", endpoint, null, callback);
    }

    public void putAsync(String endpoint, Object data, Consumer<String> callback) {
        sendAsyncWithCallback("PUT", endpoint, data, callback);
    }

    private void sendAsyncWithCallback(String method, String endpoint, Object data, Consumer<String> callback) {
        String json = (data != null) ? JsonManager.getInstance().toJson(data) : "{}";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", ACCEPT_HEADER);

        String token = SessionManager.getInstance().getJwtToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.ofString(json)).build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(res -> {
                    if (callback != null) {
                        Platform.runLater(() -> callback.accept(res));
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Errore durante la chiamata " + method + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }
}
