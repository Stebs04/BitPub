package com.bitpub.controllers;

import com.bitpub.models.*;
import com.bitpub.models.Torneo.TipoGioco;
import com.bitpub.models.Torneo.ModalitaTorneo;
import com.bitpub.network.RestClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller per la Dashboard del Gestore del locale.
 * Implementa il monitoraggio real-time tramite polling asincrono, 
 * la visualizzazione di analytics su grafici e la gestione dei tornei.
 *
 * @author Stefano Bellan
 * @version 1.0
 */
public class GestoreDashboardController {

    // --- Costanti di Configurazione ---
    private static final String ENDPOINT_MONITORAGGIO = "/gestore/monitoraggio";
    private static final String ENDPOINT_TORNEI = "/gestore/tornei";
    private static final int POLLING_INTERVAL_SECONDS = 5;

    // --- Componenti UI: Monitoraggio ---
    @FXML private TableView<Macchina> macchineTable;
    @FXML private TableColumn<Macchina, String> colMacchinaNome, colMacchinaTipo, colMacchinaStato;
    
    @FXML private TableView<Partita> partiteTable;
    @FXML private TableColumn<Partita, String> colPartitaTipo, colPartitaStato, colPartitaData;

    // --- Componenti UI: Statistiche ---
    @FXML private BarChart<String, Number> barChartPartite;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // --- Componenti UI: Tornei ---
    @FXML private TextField txtNomeTorneo;
    @FXML private ChoiceBox<TipoGioco> choiceTipoGioco;
    @FXML private ChoiceBox<ModalitaTorneo> choiceModalita;
    @FXML private DatePicker dateInizio;
    @FXML private Spinner<Integer> spinnerPartecipanti;
    @FXML private ProgressIndicator loadingIndicator;

    private final RestClient restClient = new RestClient();
    private ScheduledExecutorService scheduler;

    /**
     * Inizializza la dashboard configurando i componenti grafici e avviando
     * il ciclo di monitoraggio in background.
     */
    @FXML
    public void initialize() {
        configuraTabelle();
        configuraFormTorneo();
        avviaMonitoraggioRealTime();
    }

    /**
     * Configura il mapping dei dati per le tabelle di monitoraggio.
     */
    private void configuraTabelle() {
        colMacchinaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colMacchinaTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colMacchinaStato.setCellValueFactory(new PropertyValueFactory<>("stato"));

        colPartitaTipo.setCellValueFactory(new PropertyValueFactory<>("tipoGioco"));
        colPartitaStato.setCellValueFactory(new PropertyValueFactory<>("stato"));
        colPartitaData.setCellValueFactory(new PropertyValueFactory<>("dataInizio"));
    }

    /**
     * Predispone i selettori del form per la creazione dei tornei con i valori enumerati.
     */
    private void configuraFormTorneo() {
        choiceTipoGioco.setItems(FXCollections.observableArrayList(TipoGioco.values()));
        choiceModalita.setItems(FXCollections.observableArrayList(ModalitaTorneo.values()));
        
        spinnerPartecipanti.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 64, 8));
        dateInizio.setValue(LocalDate.now().plusDays(1));
    }

    /**
     * Avvia un executor periodico che interroga il server ogni 5 secondi.
     * Utilizza un thread separato per non bloccare la UI durante l'attesa di rete.
     */
    private void avviaMonitoraggioRealTime() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            // Chiamata di rete asincrona
            restClient.faiChiamataGet(ENDPOINT_MONITORAGGIO, DashboardGestoreData.class)
                .thenAccept(data -> {
                    // Sincronizzazione con il JavaFX Thread per aggiornare la vista
                    Platform.runLater(() -> {
                        macchineTable.getItems().setAll(data.getMacchine());
                        partiteTable.getItems().setAll(data.getPartiteRecenti());
                        aggiornaGrafico(data.getStatistiche());
                    });
                });
        }, 0, POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Gestisce la logica di creazione di un nuovo torneo.
     * Disabilita l'indicatore di caricamento al termine dell'operazione.
     */
    @FXML
    public void handleCreaTorneo() {
        Torneo nuovoTorneo = new Torneo();
        nuovoTorneo.setNome(txtNomeTorneo.getText());
        nuovoTorneo.setTipo(choiceTipoGioco.getValue());
        nuovoTorneo.setDataInizio(dateInizio.getValue());
        nuovoTorneo.setMaxPartecipanti(spinnerPartecipanti.getValue());
        nuovoTorneo.setModalita(choiceModalita.getValue());

        loadingIndicator.setVisible(true);

        restClient.faiChiamataPost(ENDPOINT_TORNEI, nuovoTorneo, String.class)
            .handle((res, ex) -> {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    if (ex == null) {
                        mostraMessaggio(Alert.AlertType.INFORMATION, "Successo", "Torneo creato con successo!");
                        resetForm();
                    } else {
                        mostraMessaggio(Alert.AlertType.ERROR, "Errore", "Errore durante la creazione del torneo.");
                    }
                });
                return null;
            });
    }

    private void aggiornaGrafico(List<StatisticaMensile> stats) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Partite Mensili");
        for (StatisticaMensile s : stats) {
            series.getData().add(new XYChart.Data<>(s.getMese(), s.getValore()));
        }
        barChartPartite.getData().setAll(series);
    }

    private void resetForm() {
        txtNomeTorneo.clear();
        dateInizio.setValue(LocalDate.now().plusDays(1));
    }

    private void mostraMessaggio(Alert.AlertType tipo, String titolo, String contenuto) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(contenuto);
        alert.showAndWait();
    }

    /**
     * Metodo di cleanup fondamentale: interrompe lo scheduler quando la vista viene distrutta.
     * Impedisce memory leak e chiamate di rete residue.
     */
    public void stopPolling() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}