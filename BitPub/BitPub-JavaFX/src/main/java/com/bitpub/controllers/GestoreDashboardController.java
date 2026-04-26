package com.bitpub.controllers;

import com.bitpub.models.*;
import com.bitpub.models.Torneo.TipoGioco;
import com.bitpub.models.Torneo.ModalitaTorneo;
import com.bitpub.network.RestClient;
import com.bitpub.network.SessionManager;
import com.bitpub.network.HttpResponseParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Arrays;
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
    @FXML private ChoiceBox<ModalitaTorneo> choiceModalita;

    @FXML private ProgressIndicator loadingIndicator;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final RestClient restClient = new RestClient();
    private Long localeId; // Caricato al login

    // Definizione DTO interna per la deserializzazione di /macchine
    public static class Macchina {
        private String nome;
        private String tipoGioco;
        private boolean attiva;

        public Macchina(String nome, String tipoGioco, boolean attiva) {
            this.nome = nome;
            this.tipoGioco = tipoGioco;
            this.attiva = attiva;
        }
        public String getNome() { return nome; }
        public String getTipoGioco() { return tipoGioco; }
        public boolean isAttiva() { return attiva; }
    }

    // Definizione DTO interna per la deserializzazione delle statistiche
    public static class StatisticheDTO {
        private long totalePartiteOggi;
        private double mediaDurataMinuti;
        private Distribuzione distribuzione;

        public static class Distribuzione {
            private long CALCIOBALILLA;
            private long FRECCETTE;
            private long BILIARDO;
        }

        public long getTotalPartiteOggi() { return totalePartiteOggi; }
        public double getMediaDurata() { return Math.round(mediaDurataMinuti * 10.0) / 10.0; }
        public long getCalciobalillaCount() { return distribuzione != null ? distribuzione.CALCIOBALILLA : 0; }
        public long getFreccetteCount() { return distribuzione != null ? distribuzione.FRECCETTE : 0; }
        public long getBiliardoCount() { return distribuzione != null ? distribuzione.BILIARDO : 0; }
    }

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
        choiceModalita.setItems(FXCollections.observableArrayList(ModalitaTorneo.values()));
        spinnerPartecipanti.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 64, 8));
        dateInizio.setValue(LocalDate.now().plusDays(1));
    }

    /**
     * Esegue il polling asincrono di macchine e partite.
     */
    private void pollData() {
        // 1. Fetch Macchine (lista di seriali come stringhe)
        restClient.faiChiamataGet("/gestore/locali/" + localeId + "/macchine", String[].class)
            .thenAccept(seriali -> {
                if (seriali != null) {
                    List<Macchina> lista = Arrays.stream(seriali)
                        .map(ser -> {
                            String tipo = ser.contains("Calciobalilla") ? "Calciobalilla" :
                                          ser.contains("Freccette") ? "Freccette" :
                                          ser.contains("Biliardo") ? "Biliardo" : "Sconosciuto";
                            return new Macchina(ser, tipo, true);
                        })
                        .toList();
                    Platform.runLater(() -> macchineTable.setItems(FXCollections.observableArrayList(lista)));
                }
            });

        // 2. Fetch Partite Attive
        restClient.faiChiamataGet("/gestore/locali/" + localeId + "/partite/attive", Partita[].class)
            .thenAccept(partite -> {
                if (partite != null) {
                    Platform.runLater(() -> partiteTable.setItems(FXCollections.observableArrayList(partite)));
                }
            });
    }

    private void loadStatistics() {
        loadingIndicator.setVisible(true);

        restClient.faiChiamataGet("/gestore/locali/" + localeId + "/statistiche", StatisticheDTO.class)
            .thenAccept(stats -> {
                if (stats != null) {
                    Platform.runLater(() -> {
                        lblPartiteOggi.setText("Partite oggi: " + stats.getTotalPartiteOggi());
                        lblDurataMedia.setText("Durata media: " + stats.getMediaDurata() + " min");
                        
                        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                            new PieChart.Data("Calciobalilla", stats.getCalciobalillaCount()),
                            new PieChart.Data("Freccette", stats.getFreccetteCount()),
                            new PieChart.Data("Biliardo", stats.getBiliardoCount())
                        );
                        tipoGiocoChart.setData(pieData);
                        loadingIndicator.setVisible(false);
                    });
                } else {
                    Platform.runLater(() -> loadingIndicator.setVisible(false));
                }
            });
    }

    @FXML
    private void handleCreaTorneo() {
        Torneo nuovoTorneo = new Torneo();
        nuovoTorneo.setNome(txtNomeTorneo.getText());
        nuovoTorneo.setTipoGioco(choiceTipoGioco.getValue());
        nuovoTorneo.setDataInizio(dateInizio.getValue());
        nuovoTorneo.setMaxPartecipanti(spinnerPartecipanti.getValue());
        nuovoTorneo.setModalita(choiceModalita.getValue());
        nuovoTorneo.setLocaleId(localeId);

        loadingIndicator.setVisible(true);
        restClient.faiChiamataPost("/gestore/tornei", nuovoTorneo, String.class)
            .handle((res, ex) -> {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    if (ex == null && res != null) {
                        showAlert(Alert.AlertType.INFORMATION, "Successo", "Torneo creato correttamente!");
                        resetForm();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Errore", "Impossibile creare il torneo.");
                    }
                });
                return null;
            });
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