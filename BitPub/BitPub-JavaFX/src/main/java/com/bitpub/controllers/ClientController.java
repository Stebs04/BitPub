package com.bitpub.controllers;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ClientController {

    // Supponiamo che questi elementi UI siano iniettati tramite FXML
    private TableView<String> tableViewEventi;
    private Button btnAzioneDinamica;

    // Il nostro client HTTP nativo per le chiamate REST
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Metodo chiamato per scaricare i dati dal Cloud Server.
     */
    public void caricaDatiDalServer() {
        // Prepariamo la richiesta, includendo il Semantic Versioning nell'Header Accept
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://indirizzo-cloud/api/v1/eventi"))
                .header("Accept", "application/vnd.bitpub.v1+json")
                .GET()
                .build();

        // Invia in modo asincrono: questo crea un THREAD IN BACKGROUND
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::elaboraRisposta) // Passa la risposta al nostro metodo
                .exceptionally(e -> {
                    System.err.println("Errore di rete: " + e.getMessage());
                    return null;
                });
    }

    /**
     * Questo metodo viene eseguito nel THREAD DI BACKGROUND.
     * Luca, qui è dove la tua review entra in gioco pesantemente!
     */
    private void elaboraRisposta(HttpResponse<String> response) {
        // ==========================================
        // PARTE DI STEFANO: Parsing JSON e HATEOAS
        // ==========================================
        // Usiamo Gson per convertire la stringa in un oggetto Json
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

        // Estraiamo i dati utili (es. il nome dell'evento)
        String nomeEvento = jsonResponse.get("evento").getAsString();

        // Estraiamo i link dinamici HATEOAS
        JsonObject links = jsonResponse.getAsJsonObject("_links");
        String nextUrl = links.getAsJsonObject("prossima_azione").get("href").getAsString();

        // ==========================================
        // PARTE DI TIMOTHY + REVIEW DI LUCA: Aggiornamento UI
        // ==========================================
        // ATTENZIONE: Se modifichiamo la UI qui fuori, l'app va in crash!
        // Dobbiamo obbligatoriamente usare Platform.runLater

        Platform.runLater(() -> {
            // TUTTO CIÒ CHE È QUI DENTRO È SICURO ED ESEGUITO NEL THREAD GRAFICO

            // 1. Aggiorniamo la tabella con i nuovi dati
            tableViewEventi.getItems().add(nomeEvento);

            // 2. Assegniamo dinamicamente il link estratto all'evento click del bottone
            btnAzioneDinamica.setOnAction(event -> {
                System.out.println("Il bottone è stato cliccato! Navigo verso: " + nextUrl);
                // Qui potresti lanciare un'altra richiesta HTTP usando nextUrl
            });

            // 3. Modifichiamo il testo del bottone per renderlo contestuale
            btnAzioneDinamica.setText("Esegui prossima azione");
        });
    }
}