package com.bitpub.controllers;

import com.bitpub.models.*;
import com.bitpub.network.RestClient;
import com.bitpub.network.SessionManager;
import com.bitpub.network.HttpResponseParser;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller per la Dashboard del Gestore.
 * Gestisce il monitoraggio real-time, le statistiche e la creazione di tornei.
 * * @author Stefano Bellan
 */
public class GestoreDashboardController {

    @FXML private TableView<Macchina> macchineTable;
    @FXML private TableColumn<Macchina, String> colMacchinaNome, colMacchinaTipo, colMacchinaStato;
    
    @FXML private TableView<Partita> partiteTable;
    @FXML private TableColumn<Partita, String> colPartitaTipo, colPartitaGiocatori, colPartitaInizio;

    @FXML private PieChart tipoGiocoChart;
    @FXML private Label lblPartiteOggi, lblDurataMedia;
    @FXML private Tab tabStatistiche;

    @FXML private TextField txtNomeTorneo;
    @FXML private ChoiceBox<TipoGioco> choiceTipoGioco;
    @FXML private DatePicker dateInizio;
    @FXML private Spinner<Integer> spinnerPartecipanti;
    @FXML private ChoiceBox<String> choiceModalita;

    @FXML private ProgressIndicator loadingIndicator;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final RestClient restClient = new RestClient();
    private final Gson gson = new Gson();
    private Long localeId; // Caricato al login

    @FXML
    public void initialize() {
        setupTables();
        setupForm();
        
        // Supponiamo che il localeId sia salvato nel SessionManager dopo il login
        this.localeId = SessionManager.getInstance().getCurrentLocaleId();

        // Avvio polling ogni 10 secondi
        scheduler.scheduleAtFixedRate(this::pollData, 0, 10, TimeUnit.SECONDS);

        // Carica statistiche quando si cambia tab
        tabStatistiche.setOnSelectionChanged(event -> {
            if (tabStatistiche.isSelected()) {
                loadStatistics();
            }
        });
    }

    private void setupTables() {
        colMacchinaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colMacchinaTipo.setCellValueFactory(new PropertyValueFactory<>("tipoGioco"));
        colMacchinaStato.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().isAttiva() ? "ONLINE" : "OFFLINE"));

        colPartitaTipo.setCellValueFactory(new PropertyValueFactory<>("tipoGioco"));
        colPartitaGiocatori.setCellValueFactory(new PropertyValueFactory<>("nomiGiocatori"));
        colPartitaInizio.setCellValueFactory(new PropertyValueFactory<>("timestampInizio"));
    }

    private void setupForm() {
        choiceTipoGioco.setItems(FXCollections.observableArrayList(TipoGioco.values()));
        choiceModalita.setItems(FXCollections.observableArrayList("INDIVIDUALE", "SQUADRE"));
        spinnerPartecipanti.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 64, 8));
        dateInizio.setValue(LocalDate.now().plusDays(1));
    }

    /**
     * Esegue il polling asincrono di macchine e partite.
     */
    private void pollData() {
        String token = SessionManager.getInstance().getJwtToken();
        
        // 1. Fetch Macchine
        restClient.getAsync("/api/v1/gestore/locali/" + localeId + "/macchine", token)
            .thenAccept(res -> {
                List<Macchina> lista = HttpResponseParser.parseList(res.body(), Macchina.class);
                // Aggiornamento UI nel thread corretto
                Platform.runLater(() -> macchineTable.setItems(FXCollections.observableArrayList(lista)));
            });

        // 2. Fetch Partite Attive
        restClient.getAsync("/api/v1/gestore/locali/" + localeId + "/partite/attive", token)
            .thenAccept(res -> {
                List<Partita> lista = HttpResponseParser.parseList(res.body(), Partita.class);
                Platform.runLater(() -> partiteTable.setItems(FXCollections.observableArrayList(lista)));
            });
    }

    private void loadStatistics() {
        loadingIndicator.setVisible(true);
        String token = SessionManager.getInstance().getJwtToken();

        restClient.getAsync("/api/v1/gestore/locali/" + localeId + "/statistiche", token)
            .thenAccept(res -> {
                // Parsing dell'oggetto statistiche (DTO personalizzato)
                StatisticheDTO stats = gson.fromJson(res.body(), StatisticheDTO.class);
                
                Platform.runLater(() -> {
                    lblPartiteOggi.setText("Partite oggi: " + stats.getTotalPartiteOggi());
                    lblDurataMedia.setText("Durata media: " + stats.getMediaDurata() + " min");
                    
                    // Aggiornamento PieChart
                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                        new PieChart.Data("Calciobalilla", stats.getCalciobalillaCount()),
                        new PieChart.Data("Freccette", stats.getFreccetteCount()),
                        new PieChart.Data("Biliardo", stats.getBiliardoCount())
                    );
                    tipoGiocoChart.setData(pieData);
                    loadingIndicator.setVisible(false);
                });
            });
    }

    @FXML
    private void handleCreaTorneo() {
        Torneo nuovoTorneo = new Torneo();
        nuovoTorneo.setNome(txtNomeTorneo.getText());
        nuovoTorneo.setTipoGioco(choiceTipoGioco.getValue());
        nuovoTorneo.setDataInizio(dateInizio.getValue().toString());
        nuovoTorneo.setMaxPartecipanti(spinnerPartecipanti.getValue());
        nuovoTorneo.setModalita(choiceModalita.getValue());
        nuovoTorneo.setLocaleId(localeId);

        String json = gson.toJson(nuovoTorneo);
        String token = SessionManager.getInstance().getJwtToken();

        loadingIndicator.setVisible(true);
        restClient.postAsync("/api/v1/gestore/tornei", json, token)
            .thenAccept(res -> Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                if (res.statusCode() == 201) {
                    showAlert(Alert.AlertType.INFORMATION, "Successo", "Torneo creato correttamente!");
                    resetForm();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Errore", "Impossibile creare il torneo.");
                }
            }));
    }

    private void resetForm() {
        txtNomeTorneo.clear();
        dateInizio.setValue(LocalDate.now().plusDays(1));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Spegne lo scheduler alla chiusura della finestra per evitare memory leak.
     */
    public void stopPolling() {
        scheduler.shutdown();
    }
}