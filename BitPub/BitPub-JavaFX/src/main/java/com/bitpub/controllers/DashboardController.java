package com.bitpub.controllers;

import com.bitpub.network.RestClient;      
import com.bitpub.network.SessionContext;  
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML
    private Label lblCredit;

    @FXML
    private Button btnFoosball;

    @FXML
    private Button btnDarts;

    @FXML
    private Button btnBilliards;

    @FXML
    private Button btnLogout;

    private final Gson gson = com.bitpub.utils.JsonManager.getGson();

    @FXML
    public void initialize() {
        // Carica il credito residuo dell'utente in un thread separato per non bloccare la UI
        new Thread(() -> {
            try {
                String responseBody = RestClient.getInstance().sendGet("/api/v1/users/me/credit");
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                String credit = json.get("credit").getAsString();
                
                // Aggiornamento della Label TASSATIVAMENTE sul thread JavaFX
                Platform.runLater(() -> lblCredit.setText("€ " + credit));
            } catch (Exception e) {
                Platform.runLater(() -> lblCredit.setText("Errore di caricamento"));
            }
        }).start();
    }

    @FXML
    void handleFoosballClick(ActionEvent event) {
        new Thread(() -> {
            try {
                // Invia la richiesta per avviare la sessione sul tavolo 1
                String payload = "{\"table_id\": 1}";
                String responseBody = RestClient.getInstance().sendPost("/api/v1/sessions/foosball/start", payload);
                
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                
                // Salva le info della sessione nel SessionContext globale
                long sessionId = json.get("id").getAsLong();
                SessionContext.setCurrentSessionId(sessionId);
                
                // Estrazione del link HATEOAS "self" per poter interrogare lo stato della sessione successivamente
                JsonObject links = json.getAsJsonObject("_links");
                if (links != null && links.has("self")) {
                    String statusUrl = links.getAsJsonObject("self").get("href").getAsString();
                    SessionContext.setCurrentSessionStatusUrl(statusUrl);
                }
                
                // Naviga al tabellone del Calciobalilla
                Platform.runLater(() -> {
                    cambiaScena(event, "/CalciobalillaUtenteView.fxml");
                });
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("409")) {
                    try {
                        String currentResponseBody = RestClient.getInstance().sendGet("/api/v1/sessions/foosball/current");
                        JsonObject currentJson = gson.fromJson(currentResponseBody, JsonObject.class);
                        long currentSessionId = currentJson.get("id").getAsLong();
                        SessionContext.setCurrentSessionId(currentSessionId);
                        
                        JsonObject currentLinks = currentJson.getAsJsonObject("_links");
                        if (currentLinks != null && currentLinks.has("self")) {
                            String currentStatusUrl = currentLinks.getAsJsonObject("self").get("href").getAsString();
                            SessionContext.setCurrentSessionStatusUrl(currentStatusUrl);
                        }
                        
                        Platform.runLater(() -> cambiaScena(event, "/CalciobalillaUtenteView.fxml"));
                    } catch (Exception ex) {
                        Platform.runLater(() -> mostraAlert("Attenzione", "Impossibile recuperare la partita in corso: " + ex.getMessage()));
                    }
                } else {
                    // Se il credito non basta o altro errore, RestClient lancerà un'eccezione
                    Platform.runLater(() -> mostraAlert("Attenzione", e.getMessage()));
                }
            }
        }).start();
    }

    @FXML
    void handleDartsClick(ActionEvent event) {
        mostraAlert("Non disponibile", "Gioco non ancora disponibile.");
    }

    @FXML
    void handleBilliardsClick(ActionEvent event) {
        mostraAlert("Non disponibile", "Gioco non ancora disponibile.");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        // Cancella i dati di sessione (JWT incluso) e torna al Login
        SessionContext.clearAll();
        cambiaScena(event, "/LoginView.fxml");
    }

    // Metodo di utility originario per la navigazione JavaFX
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
    
    // Mostra popup informativi su thread UI
    private void mostraAlert(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}
