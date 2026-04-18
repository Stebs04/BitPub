package com.bitpub.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;

public class RestClient {

    // 1. Stefano: Creazione del client HTTP (Java 11+)
    private final HttpClient httpClient;
    private final Gson gson;

    // Variabile che puoi modificare con l'indirizzo del tuo server Cloud
    private final String baseUrl = "http://localhost:8080/api";

    public RestClient() {
        this.httpClient = HttpClient.newBuilder().build();
        this.gson = new Gson(); // Inizializziamo GSON per il parsing
    }

    /**
     * Metodo per effettuare una chiamata GET asincrona a un endpoint specifico.
     * * @param endpoint L'URL finale (es. "/locali" o "/tornei")
     * @param classeDestinazione La classe in cui vogliamo convertire il JSON (es. Locale.class)
     * @return Una "Promessa" (CompletableFuture) che conterrà il nostro oggetto Java pronto
     */
    public <T> CompletableFuture<T> faiChiamataGet(String endpoint, Class<T> classeDestinazione) {

        // 2. Timothy: Creazione della richiesta con l'header obbligatorio
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .GET() // Diciamo che è una richiesta di tipo GET
                // ECCO LA TUA PARTE TIMOTHY! Iniezione dell'header per il Semantic Versioning
                .header("Accept", "application/resources.v1+json")
                .build();

        // 3. Luca: Invio asincrono e parsing con GSON
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body) // Estraiamo solo il testo JSON dalla risposta
                .thenApply(jsonText -> {
                    // Convertiamo il JSON testuale nel nostro Modello Java (es. un array di Partite)
                    return gson.fromJson(jsonText, classeDestinazione);
                });
    }
}