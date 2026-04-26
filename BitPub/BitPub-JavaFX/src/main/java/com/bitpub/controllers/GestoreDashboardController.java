package com.bitpub.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller per la Dashboard del Gestore.
 * Si occupa di caricare i dati real-time tramite polling e di creare i tornei.
 *
 * @author Stefano Bellan
 */
public class GestoreDashboardController {

    @FXML private TableView<?> macchineTable; // Aggiungi la tua classe Model al posto di <?>
    @FXML private TableView<?> partiteTable;
    @FXML private PieChart statisticheChart;
    @FXML private Label lblMediaDurata;
    
    @FXML private TextField txtNomeTorneo;
    @FXML private ChoiceBox<String> choiceTipoGioco;
    @FXML private DatePicker dateInizioTorneo;
    @FXML private TextField txtMaxPartecipanti;
    @FXML private ChoiceBox<String> choiceModalita;
    @FXML private Label lblTorneoMsg;

    private ScheduledExecutorService pollingService;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    // ID fisso per l'esempio, andrà ricavato dal SessionManager
    private final Long LOCALE_ID = 1L; 

    @FXML
    public void initialize() {
        // Inizializza i ChoiceBox
        choiceTipoGioco.setItems(FXCollections.observableArrayList("CALCIOBALILLA", "FRECCETTE", "BILIARDO"));
        choiceModalita.setItems(FXCollections.observableArrayList("INDIVIDUALE", "SQUADRE"));

        // Setup delle colonne della tabella (Mappa le proprietà della tua classe model)
        // ...
        
        avviaPolling();
    }

    /**
     * Avvia un thread in background che aggiorna i dati ogni 10 secondi.
     */
    private void avviaPolling() {
        pollingService = Executors.newSingleThreadScheduledExecutor();
        pollingService.scheduleAtFixedRate(() -> {
            caricaMacchineAttive();
            caricaPartiteAttive();
            caricaStatistiche();
        }, 0, 10, TimeUnit.SECONDS); // Delay iniziale 0, ripeti ogni 10 sec
    }

    private void caricaMacchineAttive() {
        // TODO: Recuperare il token dal tuo SessionManager
        String token = "IL_TUO_JWT_TOKEN"; 

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/v1/gestore/locali/" + LOCALE_ID + "/macchine"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/resources.v1+json")
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        // TODO: Parsing del JSON con GSON
                        
                        // L'aggiornamento UI protegge il thread JavaFX
                        Platform.runLater(() -> {
                            // macchineTable.setItems(nuoviDati);
                        });
                    }
                });
    }

    private void caricaPartiteAttive() {
        // Simile a caricaMacchineAttive() ma per l'endpoint /partite/attive
    }

    private void caricaStatistiche() {
        // Simile a caricaMacchineAttive() ma per /statistiche.
        // Nel runLater aggiorna la PieChart e la lblMediaDurata
    }

    /**
     * Chiamato quando si preme il bottone "Crea Torneo".
     */
    @FXML
    private void handleCreaTorneo() {
        // 1. Leggi i dati dal form
        String nome = txtNomeTorneo.getText();
        String tipoGioco = choiceTipoGioco.getValue();
        // ... (controlli di validazione e recupero degli altri campi)

        // 2. Crea la stringa JSON per il body (meglio usare GSON)
        String jsonBody = "{ \"nome\": \"" + nome + "\", \"tipoGioco\": \"" + tipoGioco + "\" }";

        // TODO: Recuperare token
        String token = "IL_TUO_JWT_TOKEN";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/v1/gestore/tornei"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/resources.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        lblTorneoMsg.setText("Torneo creato con successo!");
                    } else {
                        lblTorneoMsg.setStyle("-fx-text-fill: red;");
                        lblTorneoMsg.setText("Errore durante la creazione.");
                    }
                }));
    }

    /**
     * Stoppa il polling in modo pulito quando si chiude la schermata
     */
    public void stopPolling() {
        if (pollingService != null && !pollingService.isShutdown()) {
            pollingService.shutdown();
        }
    }
}