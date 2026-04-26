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
 * Gestisce l'anagrafica dei locali (CRUD) interfacciandosi con il backend 
 * tramite API REST conformi allo standard HATEOAS.
 *
 * @author Stefano Bellan
 * @version 1.1
 */
public class AdminDashboardController {

    // --- Costanti di Sistema ---
    private static final String API_BASE_URL = "http://localhost:8080/api/locali";
    private static final String MEDIA_TYPE_JSON = "application/json";

    // --- Componenti FXML ---
    @FXML private TableView<Locale> localiTable;
    @FXML private TableColumn<Locale, Long> colId;
    @FXML private TableColumn<Locale, String> colNome;
    @FXML private TableColumn<Locale, String> colCitta;
    @FXML private TableColumn<Locale, String> colIndirizzo;
    
    @FXML private Button btnModifica;
    @FXML private Button btnElimina;
    @FXML private ProgressIndicator progressIndicator;

    private final ObservableList<Locale> listaLocaliObservable = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    /**
     * Inizializza la vista configurando il data-binding della tabella e i listener di selezione.
     */
    @FXML
    public void initialize() {
        configuraTabella();
        caricaDati();

        // Listener di selezione: abilita i bottoni di modifica/eliminazione solo se una riga è selezionata.
        localiTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean rigaSelezionata = newSelection != null;
            btnModifica.setDisable(!rigaSelezionata);
            btnElimina.setDisable(!rigaSelezionata);
        });
    }

    /**
     * Configura il mapping tra le proprietà dell'oggetto Locale e le colonne della TableView.
     */
    private void configuraTabella() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colCitta.setCellValueFactory(new PropertyValueFactory<>("citta"));
        colIndirizzo.setCellValueFactory(new PropertyValueFactory<>("indirizzo"));
        localiTable.setItems(listaLocaliObservable);
    }

    /**
     * Esegue il recupero asincrono dei locali dal server.
     * Gestisce il feedback visivo tramite ProgressIndicator.
     */
    @FXML
    public void caricaDati() {
        progressIndicator.setVisible(true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL))
                .header("Accept", MEDIA_TYPE_JSON)
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken()) // Sicurezza: Iniezione Token
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::processaRispostaServer)
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        mostraNotifica("Errore Connessione", "Impossibile contattare il server: " + e.getMessage(), Alert.AlertType.ERROR);
                    });
                    return null;
                });
    }

    /**
     * Parsifica la risposta JSON (formato HATEOAS) e aggiorna la lista osservabile.
     * @param body Il corpo della risposta JSON ricevuto dal server.
     */
    private void processaRispostaServer(String body) {
        try {
            JsonObject rootObj = JsonParser.parseString(body).getAsJsonObject();
            List<Locale> localiEstratti = new ArrayList<>();

            // Navigazione del grafo HATEOAS (_embedded.localeList)
            if (rootObj.has("_embedded")) {
                JsonArray localiArray = rootObj.getAsJsonObject("_embedded").getAsJsonArray("localeList");
                for (JsonElement element : localiArray) {
                    localiEstratti.add(gson.fromJson(element, Locale.class));
                }
            }

            // Sincronizzazione con il thread UI per l'aggiornamento grafico
            Platform.runLater(() -> {
                listaLocaliObservable.setAll(localiEstratti);
                progressIndicator.setVisible(false);
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                mostraNotifica("Errore Dati", "Il server ha restituito un formato non valido.", Alert.AlertType.WARNING);
            });
        }
    }

    @FXML
    private void handleNuovoLocale() {
        // Logica per apertura dialog inserimento
        System.out.println("Apertura procedura nuovo locale...");
    }

    @FXML
    private void handleModifica() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato != null) {
            System.out.println("Modifica locale ID: " + selezionato.getId());
        }
    }

    @FXML
    private void handleElimina() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato != null) {
            // Qui andrebbe implementata la chiamata DELETE asincrona
            listaLocaliObservable.remove(selezionato);
            mostraNotifica("Successo", "Locale eliminato correttamente.", Alert.AlertType.INFORMATION);
        }
    }

    private void mostraNotifica(String titolo, String messaggio, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}