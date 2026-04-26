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
 * tramite API REST conformi allo standard HATEOAS e Semantic Versioning.
 *
 * @author Stefano Bellan
 * @version 1.0
 * @since 1.0
 */
public class AdminDashboardController {

    /** URL base per le risorse dei locali */
    private static final String API_BASE_URL = "http://localhost:8080/api/locali";
    
    /** * MediaType specifico richiesto dall'ApiVersionFilter del Cloud.
     * L'uso di application/json causerebbe un errore 406 Not Acceptable.
     */
    private static final String MEDIA_TYPE_V1 = "application/resources.v1+json";

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

        // Listener per la gestione dinamica dell'abilitazione dei controlli basata sulla selezione
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
     * Esegue il recupero asincrono dei locali dal server Cloud.
     * Inietta l'header di versione v1 per superare il filtro ApiVersionFilter.
     */
    @FXML
    public void caricaDati() {
        progressIndicator.setVisible(true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL))
                .header("Accept", MEDIA_TYPE_V1)
                .header("Authorization", "Bearer " + SessionManager.getInstance().getJwtToken())
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::processaRispostaServer)
                .exceptionally(e -> {
                    // Sincronizzazione con il JavaFX Application Thread per la manipolazione sicura dei nodi grafici
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        mostraNotifica("Errore Connessione", "Impossibile contattare il server.", Alert.AlertType.ERROR);
                    });
                    return null;
                });
    }

    /**
     * Parsifica la risposta JSON HATEOAS e aggiorna la lista osservabile.
     * @param body Il corpo della risposta JSON ricevuto dal server.
     */
    private void processaRispostaServer(String body) {
        try {
            JsonObject rootObj = JsonParser.parseString(body).getAsJsonObject();
            List<Locale> localiEstratti = new ArrayList<>();

            if (rootObj.has("_embedded")) {
                JsonArray localiArray = rootObj.getAsJsonObject("_embedded").getAsJsonArray("localeList");
                for (JsonElement element : localiArray) {
                    localiEstratti.add(gson.fromJson(element, Locale.class));
                }
            }

            Platform.runLater(() -> {
                // Aggiornamento atomico della lista per riflettere i cambiamenti nella TableView
                listaLocaliObservable.setAll(localiEstratti);
                progressIndicator.setVisible(false);
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                mostraNotifica("Errore Dati", "Formato risposta non valido.", Alert.AlertType.WARNING);
            });
        }
    }

    /**
     * Gestisce l'apertura della procedura per la creazione di un nuovo locale.
     * Metodo collegato all'onAction del file FXML.
     */
    @FXML
    public void handleNuovoLocale() {
        // Logica per apertura dialog inserimento
        System.out.println("Apertura procedura nuovo locale...");
    }

    /**
     * Gestisce la modifica del locale attualmente selezionato in tabella.
     */
    @FXML
    public void handleModifica() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato != null) {
            System.out.println("Modifica locale ID: " + selezionato.getId());
        }
    }

    /**
     * Gestisce l'eliminazione logica o fisica del locale selezionato.
     */
    @FXML
    public void handleElimina() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato != null) {
            listaLocaliObservable.remove(selezionato);
            mostraNotifica("Successo", "Locale rimosso dalla vista.", Alert.AlertType.INFORMATION);
        }
    }

    /**
     * Visualizza un alert informativo o di errore.
     * @param titolo    Titolo della finestra.
     * @param messaggio Messaggio di dettaglio.
     * @param tipo      Tipo di alert (Error, Info, Warning).
     */
    private void mostraNotifica(String titolo, String messaggio, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}