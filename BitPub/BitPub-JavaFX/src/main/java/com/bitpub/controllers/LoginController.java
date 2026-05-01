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
 * Si occupa dell'acquisizione delle credenziali utente e della comunicazione
 * asincrona con i servizi di autenticazione Cloud per l'ottenimento del token JWT.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label erroreLabel;

    /**
     * Gestisce la logica di autenticazione dell'utente.
     * Recupera l'input, valida la presenza dei campi e inoltra la richiesta al server.
     * In caso di successo, delega il reindirizzamento alla logica centralizzata del Main.
     */
    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Validazione formale dell'input per minimizzare le chiamate superflue al server
        if (username.isEmpty() || password.isEmpty()) {
            erroreLabel.setText("Inserisci username e password.");
            return;
        }

        // Feedback visivo immediato durante l'attesa della risposta dal Cloud
        erroreLabel.setText("Accesso in corso...");

        // Incapsulamento dei dati di accesso nel DTO per la richiesta REST
        AuthRequest request = new AuthRequest(username, password);

        // Chiamata asincrona POST per validare le credenziali tramite l'endpoint dedicato
        RestClient.getInstance().faiChiamataPost("/api/v1/auth/login", request, AuthResponse.class)
                .thenAccept(authResponse -> {
                    Platform.runLater(() -> {
                        // Se la risposta è valida e contiene un token
                        if (authResponse != null && authResponse.getToken() != null) {

                            // 1. Salviamo il token nel SessionManager
                            SessionManager.getInstance().setJwtToken(authResponse.getToken());

                            // ---> NUOVA RIGA DA AGGIUNGERE <---
                            // Salviamo anche il ruolo dell'utente nel SessionManager!
                            // (Nota: assumo che in AuthResponse e SessionManager i metodi si chiamino così)
                            SessionManager.getInstance().setUserRole(authResponse.getRole());

                            // 2. Deleghiamo il compito di capire quale pagina aprire al Main
                            Main.redirectDopoLogin();

                        } else {
                            erroreLabel.setText("Credenziali non valide.");
                        }
                    });
                })
                .exceptionally(e -> {
                    // Logging dell'eccezione per scopi di monitoraggio e diagnostica
                    System.err.println("Errore critico durante il processo di login:");
                    e.printStackTrace();

                    // Notifica all'utente in caso di indisponibilità dei servizi Cloud
                    Platform.runLater(() -> {
                        erroreLabel.setText("Impossibile contattare il server.");
                    });
                    return null;
                });
    }

    /**
     * Gestisce la navigazione verso la schermata di creazione di un nuovo account utente.
     */
    @FXML
    private void vaiARegistrazione() {
        Main.navigaVerso("/RegistrazioneView.fxml", "BitPub - Registrazione");
    }
}
