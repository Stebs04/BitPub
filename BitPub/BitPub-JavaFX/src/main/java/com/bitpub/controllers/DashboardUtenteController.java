package com.bitpub.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardUtenteController {

    @FXML
    void goToBiliardo(ActionEvent event) {
        cambiaScena(event, "/BiliardoUtenteView.fxml");
    }

    @FXML
    void goToCalciobalilla(ActionEvent event) {
        cambiaScena(event, "/CalciobalillaUtenteView.fxml");
    }

    @FXML
    void goToFreccette(ActionEvent event) {
        cambiaScena(event, "/FreccetteUtenteView.fxml");
    }

    private void cambiaScena(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}