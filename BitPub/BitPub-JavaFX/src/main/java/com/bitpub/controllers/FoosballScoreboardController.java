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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FoosballScoreboardController {

    @FXML
    private Label lblScoreBlue;

    @FXML
    private Label lblScoreRed;

    @FXML
    private Label lblStatus;

    @FXML
    private Button btnBackToDashboard; // Che nel FXML dovrebbe avere visibility nascosta di base

    private ScheduledExecutorService scheduler;
    private final Gson gson = new Gson();

    // Salva i punteggi precedenti per rilevare esattamente chi ha segnato tra due polling
    private int prevScoreBlue = 0;
    private int prevScoreRed = 0;

    @FXML
    public void initialize() {
        if (btnBackToDashboard != null) {
            btnBackToDashboard.setVisible(false);
        }

        // Crea uno scheduler a singolo thread
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Avvia il task di polling ogni 3 secondi senza ritardo iniziale
        scheduler.scheduleAtFixedRate(this::pollSessionState, 0, 3, TimeUnit.SECONDS);
    }

    // Metodo asincrono che interroga il Cloud
    private void pollSessionState() {
        try {
            // Usa l'URL HATEOAS salvato nel context, oppure la route standard di fallback
            String url = SessionContext.getCurrentSessionStatusUrl();
            if (url == null || url.isEmpty()) {
                url = "/api/v1/sessions/foosball/current";
            }
            
            // Richiesta HTTP GET
            String responseBody = RestClient.getInstance().sendGet(url);
            JsonObject state = gson.fromJson(responseBody, JsonObject.class);
            
            // Aggiorna l'interfaccia utente in base al JSON ricevuto
            updateUI(state);
            
        } catch (Exception e) {
            // In caso di errore (es. rete assente), stampiamo un log nel terminale.
            // Non intralciamo la UI, al prossimo polling potrebbe riprendersi.
            System.err.println("Errore durante il polling dello stato: " + e.getMessage());
        }
    }

    // Aggiorna l'interfaccia sul thread corretto
    private void updateUI(JsonObject state) {
        String status = state.has("status") && !state.get("status").isJsonNull() 
                        ? state.get("status").getAsString() : "UNKNOWN";
                        
        int scoreBlue = state.has("scoreBlue") ? state.get("scoreBlue").getAsInt() : 0;
        int scoreRed = state.has("scoreRed") ? state.get("scoreRed").getAsInt() : 0;
        
        // Potenziale campo extra nel payload per dedurre eventi come il fallo (rullata)
        String lastEvent = state.has("lastEvent") && !state.get("lastEvent").isJsonNull() 
                           ? state.get("lastEvent").getAsString() : null;

        // TASSATIVO: gli aggiornamenti UI devono avvenire sul thread JavaFX
        Platform.runLater(() -> {
            lblScoreBlue.setText(String.valueOf(scoreBlue));
            lblScoreRed.setText(String.valueOf(scoreRed));

            // Gestione dei messaggi contestuali e stato finale
            if ("FINISHED".equals(status) || "FORCE_STOPPED".equals(status)) {
                
                if ("FORCE_STOPPED".equals(status)) {
                    lblStatus.setText("PARTITA INTERROTTA FORZATAMENTE");
                } else if (scoreBlue >= 10) {
                    lblStatus.setText("VITTORIA SQUADRA BLU! 🏆");
                } else if (scoreRed >= 10) {
                    lblStatus.setText("VITTORIA SQUADRA ROSSA! 🏆");
                } else {
                    lblStatus.setText("PARTITA TERMINATA");
                }
                
                // Rendi visibile il bottone per tornare indietro e ferma il polling
                if (btnBackToDashboard != null) {
                    btnBackToDashboard.setVisible(true);
                }
                shutdown();
                
            } else if ("FOUL".equals(lastEvent)) {
                lblStatus.setText("RULLATA — Fallo!");
                resetStatusToInProgressAfterDelay();
                
            } else if (scoreBlue > prevScoreBlue) {
                lblStatus.setText("GOL SQUADRA BLU! 🔵");
                resetStatusToInProgressAfterDelay();
                
            } else if (scoreRed > prevScoreRed) {
                lblStatus.setText("GOL SQUADRA ROSSA! 🔴");
                resetStatusToInProgressAfterDelay();
                
            } else {
                lblStatus.setText("IN CORSO");
            }

            // Aggiorna la cache dei punteggi
            prevScoreBlue = scoreBlue;
            prevScoreRed = scoreRed;
        });
    }

    // Ripristina la dicitura "IN CORSO" 2 secondi dopo un gol/fallo
    private void resetStatusToInProgressAfterDelay() {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    // Evita di sovrascrivere messaggi di fine partita
                    String currentText = lblStatus.getText();
                    if (!currentText.contains("VITTORIA") && !currentText.contains("TERMINATA") && !currentText.contains("INTERROTTA")) {
                        lblStatus.setText("IN CORSO");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @FXML
    void handleBackToDashboard(ActionEvent event) {
        // Pulisce il contesto della sessione corrente
        SessionContext.setCurrentSessionId(null);
        SessionContext.setCurrentSessionStatusUrl(null);
        
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Dashboard.fxml")); // Assunto dal FILE 1
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Ferma l'executor: utile per il cleanup o al termine della partita
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
}
