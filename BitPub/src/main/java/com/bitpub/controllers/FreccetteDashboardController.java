package com.bitpub.controllers;

import com.bitpub.models.PartitaFreccette;
import com.bitpub.models.StatisticheFreccette;
import com.bitpub.network.RestClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * Controller per la Dashboard Freccette.
 * Gestisce il recupero asincrono delle statistiche aggregate interfacciandosi 
 * con il modulo unificato RestClient per il rispetto del versioning API.
 *
 * @author Timothy Giolito
 * @version 1.0
 */
public class FreccetteDashboardController {

    private static final String API_ENDPOINT_STATISTICHE = "/statistiche/freccette";
    private static final String TESTO_CARICAMENTO = "...";

    @FXML public TableView<PartitaFreccette> tabellaPartite;
    @FXML public Button bottoneAggiorna;
    @FXML public Label statoLabel;
    @FXML public Label labelTotale180;
    @FXML public Label labelMediaPunti;

    private final RestClient restClient;

    public FreccetteDashboardController() {
        this.restClient = new RestClient();
    }

    /**
     * Bootstrap dello stato iniziale della dashboard.
     */
    @FXML
    public void initialize() {
        caricaStatisticheFreccette();
    }

    /**
     * Esegue una chiamata asincrona per recuperare le statistiche aggiornate.
     * Sincronizza i dati ricevuti con il thread grafico JavaFX.
     */
    @FXML
    public void caricaStatisticheFreccette() {
        Platform.runLater(() -> {
            labelTotale180.setText(TESTO_CARICAMENTO);
            statoLabel.setText("Aggiornamento in corso...");
            bottoneAggiorna.setDisable(true);
        });

        restClient.faiChiamataGet(API_ENDPOINT_STATISTICHE, StatisticheFreccette.class)
                .thenAccept(statistiche -> {
                    // Riversamento sicuro dei risultati sul JavaFX Application Thread
                    Platform.runLater(() -> {
                        labelTotale180.setText(String.valueOf(statistiche.getTotale180()));
                        labelMediaPunti.setText(String.format("%.2f", statistiche.getMediaPuntiTorneo()));
                        statoLabel.setText("Dati sincronizzati.");
                        bottoneAggiorna.setDisable(false);
                    });
                })
                .exceptionally(errore -> {
                    Platform.runLater(() -> {
                        statoLabel.setText("Errore di sincronizzazione.");
                        bottoneAggiorna.setDisable(false);
                    });
                    return null;
                });
    }
}