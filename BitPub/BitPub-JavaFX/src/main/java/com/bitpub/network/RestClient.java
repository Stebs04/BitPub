package com.bitpub.network;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Service Layer per la gestione delle comunicazioni HTTP verso il Cloud BitPub.
 * Questa classe incapsula la logica di rete, gestendo la serializzazione JSON
 * e le chiamate asincrone per non bloccare il thread principale della UI.
 *
 *
 * @author Stefano Bellan, Timothy Giolito, Luca Franzon
 */
public class RestClient {

    // --- LOGICA DI STEFANO: Core Engine ---
    private final HttpClient httpClient;
    private final Gson gson;

    /** URL base dell'API di backend (Spring Boot) */
    private final String baseUrl = "http://localhost:8080/api";

    // --- LOGICA DI TIMOTHY: Versioning e Headers ---
    /** Header specifico per il Semantic Versioning richiesto dalla Fase 13 */
    private static final String ACCEPT_HEADER = "application/resources.v1+json";

    /**
     * Costruttore predefinito. Inizializza il client HTTP nativo (Java 11+)
     * e l'istanza GSON per il mapping degli oggetti.
     */
    public RestClient() {
        // Stefano: Configurazione del client nativo ad alte prestazioni
        this.httpClient = HttpClient.newBuilder().build();
        this.gson = new Gson();
    }

    /**
     * Esegue una richiesta GET asincrona per il recupero di risorse.
     *
     * @param <T>           Tipo generico della risposta attesa.
     * @param endpoint      Il percorso relativo della risorsa (es. "/calciobalilla").
     * @param tipoRisposta  La classe di destinazione per il mapping JSON.
     * @return Un {@link CompletableFuture} che restituirà l'oggetto mappato una volta completata la richiesta.
     */
    public <T> CompletableFuture<T> faiChiamataGet(String endpoint, Class<T> tipoRisposta) {

        // 1. STEFANO & TIMOTHY: Costruzione della Request con iniezione del versioning nell'Header
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .GET()
                .header("Accept", ACCEPT_HEADER) // Timothy: Garantisce la compatibilità v1
                .build();

        // 2. LUCA: Orchestrazione asincrona del flusso dati
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                // Estrazione del corpo della risposta HTTP (Stringa JSON)
                .thenApply(HttpResponse::body)
                // Luca: Conversione del testo JSON in oggetto Java tramite GSON
                .thenApply(jsonText -> gson.fromJson(jsonText, tipoRisposta));
    }

    /**
     * Esegue una richiesta POST asincrona per la creazione di nuove risorse o invio dati.
     *
     * @param <T>           Tipo generico della risposta attesa dal server.
     * @param endpoint      Il percorso relativo per l'invio dati (es. "/partite/salva").
     * @param datiDaInviare L'oggetto Java da serializzare in JSON.
     * @param tipoRisposta  La classe di destinazione per il mapping della risposta.
     * @return Un {@link CompletableFuture} contenente la risposta del server.
     */
    public <T> CompletableFuture<T> faiChiamataPost(String endpoint, Object datiDaInviare, Class<T> tipoRisposta) {

        // Trasformazione del Payload: da POJO a stringa JSON
        String jsonDaSpedire = gson.toJson(datiDaInviare);

        // Configurazione della richiesta di tipo POST con Content-Type appropriato
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Accept", ACCEPT_HEADER)
                .header("Content-Type", "application/json") // Specifica il formato del payload inviato
                .POST(HttpRequest.BodyPublishers.ofString(jsonDaSpedire))
                .build();

        // Esecuzione e mapping asincrono della risposta
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(jsonText -> gson.fromJson(jsonText, tipoRisposta));
    }
}
