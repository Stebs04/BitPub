package com.bitpub.controllers;

import com.bitpub.models.Locale;
import com.bitpub.network.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller per la Dashboard Amministratore.
 * Gestisce la visualizzazione, creazione, modifica ed eliminazione dei locali
 * comunicando con il backend tramite API REST HATEOAS.
 *
 * @author Stefano Bellan
 * @version 1.0
 */
public class AdminDashboardController {

    @FXML private TableView<Locale> tabellaLocali;
    @FXML private TableColumn<Locale, Long> colonnaId;
    @FXML private TableColumn<Locale, String> colonnaNome;
    @FXML private TableColumn<Locale, String> colonnaCitta;
    @FXML private TableColumn<Locale, String> colonnaIndirizzo;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button btnModifica;
    @FXML private Button btnElimina;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private ObservableList<Locale> listaLocaliObservable = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configurazione delle colonne della tabella
        colonnaId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colonnaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colonnaCitta.setCellValueFactory(new PropertyValueFactory<>("citta"));
        colonnaIndirizzo.setCellValueFactory(new PropertyValueFactory<>("indirizzo"));

        tabellaLocali.setItems(listaLocaliObservable);

        // Abilita i pulsanti Modifica/Elimina solo se c'è una riga selezionata
        tabellaLocali.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean isSelected = newSelection != null;
            btnModifica.setDisable(!isSelected);
            btnElimina.setDisable(!isSelected);
        });

        caricaLocali();
    }

    /**
     * Effettua una chiamata HTTP GET asincrona per recuperare la lista dei locali.
     * Estrapola i dati dal payload HATEOAS e aggiorna l'interfaccia utente.
     */
    @FXML
    public void caricaLocali() {
        progressIndicator.setVisible(true);

        String token = SessionManager.getInstance().getJwtToken();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/v1/admin/locali"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/resources.v1+json")
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::parsificaLocaliDaHateoas)
                .exceptionally(e -> {
                    // Proteggiamo l'aggiornamento UI delegandolo al thread JavaFX
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        mostraErrore("Errore di Rete", "Impossibile recuperare i dati dei locali.");
                    });
                    return null;
                });
    }

    /**
     * Parsifica la risposta JSON in formato HAL/HATEOAS ed estrae gli oggetti Locale.
     * * @param jsonResponse Il body della risposta HTTP in formato JSON
     */
    private void parsificaLocaliDaHateoas(String jsonResponse) {
        List<Locale> localiEstratti = new ArrayList<>();
        try {
            JsonObject rootObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            // Verifica la presenza del nodo _embedded tipico di Spring Data REST
            if (rootObj.has("_embedded")) {
                JsonObject embedded = rootObj.getAsJsonObject("_embedded");
                if (embedded.has("localeList")) {
                    JsonArray localiArray = embedded.getAsJsonArray("localeList");
                    for (JsonElement element : localiArray) {
                        Locale locale = gson.fromJson(element, Locale.class);
                        localiEstratti.add(locale);
                    }
                }
            }
            
            // Platform.runLater garantisce la thread-safety per l'interfaccia grafica
            Platform.runLater(() -> {
                listaLocaliObservable.setAll(localiEstratti);
                progressIndicator.setVisible(false);
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                mostraErrore("Errore di Parsing", "Formato dati non valido dal server.");
            });
        }
    }

    // Metodi per mostraDialogLocale() e eliminaLocale() sarebbero qui implementati
    // seguendo lo stesso pattern di httpClient.sendAsync() e Platform.runLater()...

    private void mostraErrore(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}