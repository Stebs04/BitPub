package it.unibo.bitpub.javafx.network;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import com.google.gson.Gson;
import it.unibo.bitpub.cloud.model.BiliardoResource; // Assicurati di importare il modello corretto

public class BiliardoApiClient {

    // L'HttpClient nativo di Java 11+
    private final HttpClient httpClient;
    // GSON per la deserializzazione
    private final Gson gson;

    public BiliardoApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Esegue una richiesta HTTP in background e converte il JSON di risposta.
     * * @param request La richiesta HTTP preparata da Stefano/Timothy.
     * @return Un CompletableFuture che conterrà la risorsa Biliardo.
     */
    public CompletableFuture<BiliardoResource> eseguiRichiestaBiliardo(HttpRequest request) {

        // 1. Inviamo la richiesta in modo asincrono.
        // BodyHandlers.ofString() dice al client di leggere la risposta come testo.
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())

                // 2. Quando arriva la risposta, estraiamo solo il corpo (il JSON)
                .thenApply(HttpResponse::body)

                // 3. Quando abbiamo il JSON, usiamo GSON per convertirlo nell'oggetto Java
                .thenApply(jsonBody -> {
                    // Deserializzazione: da Stringa JSON a Oggetto Java
                    return gson.fromJson(jsonBody, BiliardoResource.class);
                });
    }
}