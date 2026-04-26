package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.model.BiliardoStatistiche;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;

/**
 * Controller per la gestione della UI relativa alle statistiche del Biliardo.
 * Implementa chiamate non bloccanti per garantire la reattività della dashboard locale.
 *
 * @author Luca Franzon
 * @version 1.0
 */
public class BiliardoUIController {

    private static final String API_ENDPOINT = "/biliardo/statistiche";

    @FXML private Label serieMassimaLabel;
    @FXML private ListView<String> storicoListView;
    @FXML private Label statoLabel;
    @FXML private Button btnAggiorna;

    private RestClient restClient;

    @FXML
    public void initialize() {
        this.restClient = new RestClient();
        caricaDatiBiliardo();
    }

    /**
     * Recupera le statistiche aggiornate dal backend Cloud.
     * Gestisce rigorosamente il rinfresco della UI tramite il JavaFX Application Thread.
     */
    @FXML
    public void caricaDatiBiliardo() {
        Platform.runLater(() -> {
            btnAggiorna.setDisable(true);
            statoLabel.setText("Caricamento statistiche...");
        });

        restClient.faiChiamataGet(API_ENDPOINT, BiliardoStatistiche.class)
                .thenAccept(statistiche -> {
                    // L'aggiornamento della ListView richiede l'accesso esclusivo al thread grafico
                    Platform.runLater(() -> {
                        serieMassimaLabel.setText(String.valueOf(statistiche.getSerieMassimaPalle()));
                        storicoListView.getItems().setAll(statistiche.getStoricoPartite());
                        statoLabel.setText("Dati pronti.");
                        btnAggiorna.setDisable(false);
                    });
                })
                .exceptionally(errore -> {
                    Platform.runLater(() -> {
                        statoLabel.setText("Rete non disponibile.");
                        btnAggiorna.setDisable(false);
                    });
                    return null;
                });
    }
}