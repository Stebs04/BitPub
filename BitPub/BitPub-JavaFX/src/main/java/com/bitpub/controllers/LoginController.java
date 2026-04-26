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
 * Gestisce l'interfaccia di login, l'invio asincrono delle credenziali al backend,
 * e l'inizializzazione della sessione utente (JWT) in caso di successo.
 *
 * @author Stefano Bellan 20054330
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label erroreLabel;

    private static final String API_URL = "http://localhost:8080/api/v1/auth/login";
    private final HttpClient httpClient;
    private final Gson gson;

    /**
     * Costruttore predefinito.
     * Inizializza il client HTTP nativo e il parser JSON.
     */
    public LoginController() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.gson = new Gson();
    }

    /**
     * Gestisce l'evento scatenato dal click sul pulsante "Accedi".
     * Esegue la validazione dei campi e inoltra in modo asincrono la richiesta di login.
     *
     * @param event L'evento di interazione dell'utente.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            erroreLabel.setText("Inserisci username e password");
            return;
        }

        // Costruzione del payload tipizzato
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

        // Esecuzione asincrona: non blocchiamo mai il thread UI di JavaFX durante il traffico di rete
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // Platform.runLater assicura che gli aggiornamenti visivi avvengano sul JavaFX Application Thread
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            // Deserializzazione del JWT e dei metadati associati
                            AuthResponse authResponse = gson.fromJson(response.body(), AuthResponse.class);
                            
                            // Inizializzazione protetta della sessione utente in memoria
                            SessionManager.getInstance().setSession(
                                    authResponse.getUsername(),
                                    authResponse.getToken(),
                                    authResponse.getRuolo(),
                                    null // Locale non è strettamente necessario al primo livello di login
                            );
                            
                            // Naviga verso l'interfaccia principale post-login
                            Main.navigaVerso("/MainView.fxml", "BitPub - Dashboard");
                        } else {
                            erroreLabel.setText("Credenziali errate o utente non trovato.");
                        }
                    });
                })
                .exceptionally(ex -> {
                    // Gestione fail-safe per problemi di raggiungibilità del backend cloud
                    // Platform.runLater protegge l'accesso alla Label dal Worker Thread
                    Platform.runLater(() -> erroreLabel.setText("Errore di connessione al server: " + ex.getMessage()));
                    return null;
                });
    }

    /**
     * Reindirizza l'utente alla schermata di registrazione.
     *
     * @param event L'evento generato dal click sul link.
     */
    @FXML
    public void vaiARegistrazione(ActionEvent event) {
        Main.navigaVerso("/RegistrazioneView.fxml", "BitPub - Registrazione");
    }
}