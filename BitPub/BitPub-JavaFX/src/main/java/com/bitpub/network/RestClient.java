package com.bitpub.network;

import com.bitpub.utils.JsonManager;
import javafx.application.Platform;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private static RestClient instance;
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
        this.client = HttpClient.newHttpClient();
    }

    /**
     * Recupera l'istanza unica del client (Thread-safe).
     */
    public static synchronized RestClient getInstance() {
        if (instance == null) {
            instance = new RestClient();
        }
        return instance;
    }

    /**
     * Metodo per la compatibilità con i controller Gestore e Admin.
     * Timothy: Inserisce il versioning e il token JWT recuperato dal SessionManager.
     *
     * @return Un {@link CompletableFuture} con l'oggetto mappato.
     */
    public <T> CompletableFuture<T> faiChiamataGet(String endpoint, Class<T> responseClass) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Accept", ACCEPT_HEADER)
                .GET();

        // Iniezione automatica del Token se presente
        String token = SessionManager.getInstance().getJwtToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();

        // Luca: Gestione del flusso dati asincrono
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // --- NUOVO: DEBUG DELLA RISPOSTA ---
                    System.out.println("=== DEBUG CHIAMATA GET: " + endpoint + " ===");
                    System.out.println("Status Code: " + response.statusCode());
                    System.out.println("Raw JSON: " + response.body());
                    System.out.println("=========================================");
                    
                    // Lanciamo un'eccezione se il server risponde con un errore (es. 401, 403, 500)
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Errore HTTP dal server: " + response.statusCode());
                    }

                    // Tenta la conversione (qui è dove probabilmente avviene il crash)
                    return JsonManager.getInstance().fromJson(response.body(), responseClass);
                });
    }

    /**
     * Esegue una richiesta POST asincrona restituendo un CompletableFuture.
     * Richiesto dal GestoreDashboardController.
     */
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
                .thenApply(response -> JsonManager.getInstance().fromJson(response.body(), responseClass));
    }

    /**
     * Esegue una POST asincrona con callback (ottimizzata per il modulo Calciobalilla).
     */
    public void postAsync(String endpoint, Object data, Consumer<String> callback) {
        sendAsyncWithCallback("POST", endpoint, data, callback);
    }

    /**
     * Esegue una DELETE asincrona con callback.
     *
     * @param endpoint Percorso relativo dell'API (es. "/api/v1/locali/5").
     * @param callback Azione da eseguire sul thread JavaFX al completamento.
     */
    public void deleteAsync(String endpoint, Consumer<String> callback) {
        sendAsyncWithCallback("DELETE", endpoint, null, callback);
    }

    /**
     * Esegue una PUT asincrona con callback (ottimizzata per il modulo Calciobalilla).
     */
    public void putAsync(String endpoint, Object data, Consumer<String> callback) {
        sendAsyncWithCallback("PUT", endpoint, data, callback);
    }

    /**
     * LUCA: Orchestrazione asincrona del flusso dati con ritorno su thread JavaFX.
     * Assicura che la callback venga eseguita correttamente per aggiornare la UI.
     */
    private void sendAsyncWithCallback(String method, String endpoint, Object data, Consumer<String> callback) {
        // Stefano: Trasformazione del Payload (se nullo, invia oggetto vuoto)
        String json = (data != null) ? JsonManager.getInstance().toJson(data) : "{}";

        // Timothy: Configurazione Headers con supporto al Versioning v1
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", ACCEPT_HEADER);

        String token = SessionManager.getInstance().getJwtToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.ofString(json)).build();

        // Luca: Invio non bloccante e riallineamento al thread UI di JavaFX tramite Platform.runLater
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