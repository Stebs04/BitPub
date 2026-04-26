package com.bitpub.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.scene.control.Alert;

/**
 * Controller per la registrazione utente
 *
 * @author BitPub Team
 * @version 1.0
 */
public class RegistrazioneController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confermaPasswordField;
    @FXML private Label erroreLabel;

    private static final String API_URL = "http://localhost:8080/api/v1/auth/register";
    private HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void handleRegistrazione(ActionEvent event) {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String conferma = confermaPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || conferma.isEmpty()) {
            erroreLabel.setText("Compila tutti i campi");
            return;
        }

        if (!password.equals(conferma)) {
            erroreLabel.setText("Le password non coincidono");
            return;
        }

        String jsonBody = String.format("{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", username, email, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/resources.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Operazione di rete protetta asincrona in JavaFX
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 201) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Successo");
                            alert.setHeaderText(null);
                            alert.setContentText("Registrazione completata!");
                            alert.showAndWait();
                            // Qui integreremmo la navigazione al login
                        } else {
                            erroreLabel.setText("Errore: " + response.body());
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> erroreLabel.setText("Errore di connessione"));
                    return null;
                });
    }

    @FXML
    public void tornaAlLogin(ActionEvent event) {
        // Da implementare la navigazione
    }
}