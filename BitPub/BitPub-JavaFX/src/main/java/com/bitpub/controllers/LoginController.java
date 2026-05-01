package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.network.RestClient;
import com.bitpub.network.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller per la gestione della vista di Login.
 * Gestisce l'interazione con l'utente per l'autenticazione, interfacciandosi
 * con il server Cloud e coordinando il reindirizzamento post-login basato sui ruoli.
 * 
 * @author Stefano Bellan 20054330
 * @since 2024
 */
public class LoginController {

    @FXML private TextField usernameField;   // Collegato al componente fx:id="usernameField"
    @FXML private PasswordField passwordField;
    @FXML private Label erroreLabel;          // Collegato al componente fx:id="erroreLabel"

    /**
     * Innesca la procedura di autenticazione.
     * Recupera le credenziali dai campi di testo e invia una richiesta POST asincrona al server.
     */
    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Validazione formale dell'input locale
        if (username.isEmpty() || password.isEmpty()) {
            erroreLabel.setText("Inserisci username e password.");
            return;
        }

        // Reset dei messaggi di errore precedenti
        erroreLabel.setText("");

        // Creazione del DTO per la richiesta di autenticazione
        AuthRequest request = new AuthRequest(username, password);

        // Chiamata REST asincrona tramite il client centralizzato
        RestClient.getInstance().faiChiamataPost("/api/v1/auth/login", request, AuthResponse.class)
                .thenAccept(response -> {
                    if (response != null && response.getToken() != null) {
                        // 1. Persistenza dei metadati di sessione nel Singleton locale
                        SessionManager.getInstance().setJwtToken(response.getToken());
                        SessionManager.getInstance().setUsername(response.getUsername());
                        
                        // 2. Memorizzazione del ruolo per la logica di routing centralizzata
                        SessionManager.getInstance().setUserRole(response.getRole());
                        
                        // 3. Esecuzione del redirect sul thread JavaFX Application
                        Platform.runLater(Main::redirectDopoLogin);
                    } else {
                        // Gestione errore credenziali (es. 401 Unauthorized)
                        Platform.runLater(() ->
                            erroreLabel.setText("Credenziali errate o utente non autorizzato."));
                    }
                })
                .exceptionally(ex -> {
                    // Gestione fallimento connessione o eccezioni di rete
                    Platform.runLater(() ->
                        erroreLabel.setText("Errore di rete: impossibile contattare il server."));
                    return null;
                });
    }

    /**
     * Reindirizza l'utente alla schermata di registrazione.
     */
    @FXML
    private void vaiARegistrazione() {
        Main.navigaVerso("/RegistrazioneView.fxml", "BitPub - Registrazione");
    }
}
