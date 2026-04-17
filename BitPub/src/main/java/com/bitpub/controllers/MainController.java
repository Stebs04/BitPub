package com.bitpub.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;

    /**
     * Metodo generico per caricare una vista FXML nell'area centrale.
     */
    private void loadView(String fxmlFileName) {
        try {
            // Carica il file FXML dalla cartella delle risorse
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bitpub/views/" + fxmlFileName));
            Parent view = loader.load();

            // Sostituisce il contenuto attuale dell'area centrale
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Errore nel caricamento della vista: " + fxmlFileName);
            e.printStackTrace();
        }
    }

    @FXML
    private void mostraDashboard() {
        // loadView("DashboardGenerale.fxml");
    }

    @FXML
    private void mostraFreccette() {
        // Carica la dashboard che hai creato tu, Timothy!
        loadView("FreccetteDashboard.fxml");
    }

    @FXML
    private void mostraCalciobalilla() {
        loadView("CalciobalillaDashboard.fxml");
    }

    @FXML
    private void mostraBiliardo() {
        loadView("BiliardoDashboard.fxml");
    }
}