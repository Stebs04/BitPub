package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.network.SessionManager;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Controller per la gestione dell'autenticazione utente.
 * Gestisce l'interfaccia di login e il reindirizzamento dinamico basato sui ruoli.
 *
 * @author Stefano Bellan
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label erroreLabel;

    private static final String API_URL = "http://localhost:8080/api/v1/auth/login";
    private final HttpClient httpClient;
    private final Gson gson;

    public LoginController() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.gson = new Gson();
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            erroreLabel.setText("Inserisci username e password");
            return;
        }

        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername(username.trim());
        authRequest.setPassword(password);
        String jsonBody = gson.toJson(authRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/resources.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            AuthResponse authResponse = gson.fromJson(response.body(), AuthResponse.class);
                            
                            // Salvataggio della sessione nel SessionManager globale
                            SessionManager.getInstance().setSession(
                                    authResponse.getUsername(),
                                    authResponse.getToken(),
                                    authResponse.getRuolo(),
                                    null
                            );
                            
                            // DELEGA AL MAIN: Smistamento basato sul ruolo (Admin, Gestore o Utente)
                            Main.redirectDopoLogin();
                        } else {
                            erroreLabel.setText("Credenziali errate.");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> erroreLabel.setText("Connessione fallita."));
                    return null;
                });
    }

    @FXML
    public void vaiARegistrazione(ActionEvent event) {
        Main.navigaVerso("/RegistrazioneView.fxml", "BitPub - Registrazione");
    }
}