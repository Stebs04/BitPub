package it.unibo.bitpub.javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.application.Platform;

public class CalciobalillaUIController {

    @FXML
    private Label punteggioRosso;

    @FXML
    private Label punteggioBlu;

    private int scoreRosso = 0;

    @FXML
    public void simulaGol() {
        // Simulazione di una chiamata HTTP asincrona (HttpClient)
        new Thread(() -> {
            scoreRosso++;

            // Evitiamo il crash NotOnFxApplicationThread delegando al thread grafico
            Platform.runLater(() -> {
                punteggioRosso.setText(String.valueOf(scoreRosso));
            });
        }).start();
    }
}