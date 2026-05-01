package com.bitpub.network;

import javafx.application.Platform;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Asynchronous HTTP service for JavaFX applications.
 * Manages the full lifecycle of non-blocking HTTP requests,
 * ensuring all UI callbacks are dispatched on the JavaFX Application Thread.
 *
 * <p>Thread model:
 * <pre>
 *  JavaFX AT  ──► sendAsync() ──► Worker Thread (parse) ──► Platform.runLater ──► JavaFX AT
 * </pre>
 *
 * @author Stefano Bellan 20054330
 * @version 1.0
 */
public class AsyncHttpService {

    /** Shared HttpClient instance – thread-safe by design (Java 11+) */
    private final HttpClient httpClient;

    /**
     * Default constructor. Initializes the native Java HTTP client.
     */
    public AsyncHttpService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * Sends an asynchronous HTTP request, parses the response on a worker thread,
     * and delivers the result to the JavaFX Application Thread via Platform.runLater().
     *
     * @param <T>       the type of the parsed response object
     * @param request   the HTTP request to send
     * @param parser    a function that transforms the raw response body into type T;
     *                  executed on the worker thread – must be thread-safe and stateless
     * @param onSuccess consumer invoked with the parsed result on the JavaFX Application Thread
     * @param onError   consumer invoked with the throwable on the JavaFX Application Thread
     * @return a CompletableFuture representing the async operation (can be cancelled)
     */
    public <T> CompletableFuture<Void> sendAsync(
            HttpRequest request,
            Function<HttpResponse<String>, T> parser,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError) {

        // Initiate the asynchronous network call. The ForkJoinPool handles background threads.
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                // Parse response on worker thread – never blocks JavaFX AT
                .thenApply(parser)
                // Deliver result to JavaFX AT safely
                .thenAccept(result -> 
                    // UI update – must run on JavaFX Application Thread
                    Platform.runLater(() -> onSuccess.accept(result))
                )
                .exceptionally(ex -> {
                    // Route errors to JavaFX AT for UI feedback
                    Platform.runLater(() -> onError.accept(ex));
                    return null;
                });
    }

    /**
     * Invia in modo asincrono i dati per la creazione di un nuovo locale al Cloud.
     * È essenziale definire l'header Accept per il Semantic Versioning.
     *
     * @param jsonPayload I dati del locale serializzati in JSON
     * @param jwtToken    Il token di autenticazione dell'admin
     * @return Future della risposta HTTP
     */
    public CompletableFuture<HttpResponse<String>> creaLocaleAsincrono(String jsonPayload, String jwtToken, String baseUrl) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/api/locali")) // O adatta al path
                .header("Authorization", "Bearer " + jwtToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/resources.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Invia un aggiornamento HTTP PUT in modo asincrono.
     * Utilizzato per aggiornare risorse remote sul Cloud.
     * 
     * @param endpoint L'URI relativo per l'endpoint (es. /api/locali/1)
     * @param jsonPayload I dati in formato JSON per l'aggiornamento
     * @param jwtToken Il token JWT dell'utente loggato
     * @return CompletableFuture con la risposta HTTP restituita
     */
    public CompletableFuture<HttpResponse<String>> putAsync(String endpoint, String jsonPayload, String jwtToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8080" + endpoint))
                .header("Authorization", "Bearer " + jwtToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/resources.v1+json") // Mantieni il versioning richiesto!
                .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload)) // Nota: usa .PUT
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
