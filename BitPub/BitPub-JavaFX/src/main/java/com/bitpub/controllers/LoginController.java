package com.bitpub.controllers;

import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.network.RestClient;
import com.bitpub.network.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controller per la gestione dell'autenticazione degli amministratori.
 * Gestisce l'invio delle credenziali al server Cloud e il caricamento del layout principale.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator loader;

    /**
     * Gestisce l'evento di login. Valida l'input e avvia la richiesta asincrona al server.
     */
    @FXML
    public void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        // Validazione formale dell'input locale
        if (email.isEmpty() || password.isEmpty()) {
            mostraErrore("Campi mancanti", "Inserisci email e password.");
            return;
        }

        // Feedback visivo: attivazione loader e disabilitazione pulsante
        loader.setVisible(true);
        loginButton.setDisable(true);

        // Creazione del DTO per la richiesta di autenticazione
        AuthRequest request = new AuthRequest(email, password);

        // Chiamata asincrona al server Cloud tramite RestClient Singleton
        RestClient.getInstance().faiChiamataPost("/api/v1/auth/login", request, AuthResponse.class)
                .thenAccept(response -> {
                    if (response != null && response.getToken() != null) {
                        // 1. Salvataggio persistente del token JWT e del nome utente nella sessione
                        SessionManager.getInstance().setJwtToken(response.getToken());
                        SessionManager.getInstance().setUsername(response.getUsername());

                        // 2. Reindirizzamento alla Dashboard nel thread grafico
                        Platform.runLater(this::apriDashboard);
                    } else {
                        // Gestione fallimento autenticazione (es. 401 Unauthorized)
                        Platform.runLater(() -> {
                            loader.setVisible(false);
                            loginButton.setDisable(false);
                            mostraErrore("Login Fallito", "Credenziali errate o utente non autorizzato.");
                        });
                    }
                })
                .exceptionally(ex -> {
                    // Gestione errori di rete o timeout del server
                    Platform.runLater(() -> {
                        loader.setVisible(false);
                        loginButton.setDisable(false);
                        mostraErrore("Errore di Rete", "Impossibile contattare il server.");
                    });
                    return null;
                });
    }

    /**
     * Carica il layout principale dell'area amministrativa (AdminMainLayout.fxml).
     * Sostituisce la scena attuale sullo stage principale.
     */
    private void apriDashboard() {
        try {
            // Caricamento del layout radice con Sidebar e area di contenuto dinamica
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bitpub/views/AdminMainLayout.fxml"));
            Parent root = loader.load();

            // Ottenimento dello stage corrente e switch della scena
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("BitPub Admin Panel - " + SessionManager.getInstance().getUsername());
            stage.centerOnScreen();

        } catch (Exception e) {
            // Log dell'eccezione in caso di file FXML mancante o errato
            e.printStackTrace();
            mostraErrore("Errore Caricamento", "Impossibile aprire la Dashboard.");
        }
    }

    /**
     * Utility per la visualizzazione di finestre di dialogo (Alert) di errore.
     *
     * @param titolo Il titolo della finestra.
     * @param msg Il contenuto del messaggio di errore.
     */
    private void mostraErrore(String titolo, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
