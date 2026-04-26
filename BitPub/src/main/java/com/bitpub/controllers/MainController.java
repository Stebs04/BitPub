package com.bitpub.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;

/**
 * Controller del layout principale. Gestisce lo swapping delle viste interne.
 * * @author Stefano Bellan
 */
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard, btnCalciobalilla, btnFreccette, btnBiliardo;

    @FXML
    public void initialize() {
        apriDashboard(); // Carica la home al primo avvio
    }

    @FXML
    public void apriDashboard() {
        caricaSottoModulo("DashboardView.fxml");
    }

    @FXML
    public void apriCalciobalilla() {
        // Usa il file unificato corretto
        caricaSottoModulo("CalciobalillaGestione.fxml");
    }

    @FXML
    public void apriFreccette() {
        /** @author Timothy Giolito - Versione 1.0 */
        caricaSottoModulo("FreccetteView.fxml");
    }

    @FXML
    public void apriBiliardo() {
        /** @author Luca Franzon - Versione 1.0 */
        caricaSottoModulo("BiliardoView.fxml");
    }

    /**
     * Metodo centralizzato per il caricamento dinamico dei moduli nell'area centrale.
     */
    private void caricaSottoModulo(String fxml) {
        try {
            var resource = getClass().getResource("/" + fxml);
            if (resource == null) {
                System.err.println("Modulo non trovato: " + fxml);
                return;
            }
            Node view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);
            
            // Logica inline: Platform.runLater non serve qui se siamo già sul thread UI
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}