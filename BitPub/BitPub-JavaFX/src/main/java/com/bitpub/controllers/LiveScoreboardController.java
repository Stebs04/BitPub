package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class LiveScoreboardController implements Initializable {

    @FXML private Label liveIndicator;
    @FXML private Label gameNameLabel;
    @FXML private Label player1Label;
    @FXML private Label score1Label;
    @FXML private Label player2Label;
    @FXML private Label score2Label;
    @FXML private Label statusMessageLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button closeButton;

    private String sessionId;
    private Runnable onMatchEndedCallback;
    private Timeline pollingTimeline;
    private boolean isIndicatorVisible = true;

    public void initData(String gameName, String sessionId, Runnable onMatchEndedCallback) {
        this.gameNameLabel.setText(gameName);
        this.sessionId = sessionId;
        this.onMatchEndedCallback = onMatchEndedCallback;
        startPolling();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Animation for the LIVE indicator
        Timeline blinker = new Timeline(new KeyFrame(Duration.seconds(0.8), e -> {
            isIndicatorVisible = !isIndicatorVisible;
            liveIndicator.setVisible(isIndicatorVisible);
        }));
        blinker.setCycleCount(Animation.INDEFINITE);
        blinker.play();
    }

    private void startPolling() {
        // Polling asincrono ogni 2 secondi
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> pollBackend()));
        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();
    }

    private void pollBackend() {
        // Effettua la richiesta HTTP asincrona (in background)
        RestClient.getInstance().getAsync(RestClient.getInstance().getRootUrl() + "/api/v1/sessions/active/" + sessionId, String.class)
            .thenAccept(response -> {
                // Aggiorna la UI in modo sicuro sul thread JavaFX
                Platform.runLater(() -> {
                    try {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        
                        if (json.has("score1")) score1Label.setText(json.get("score1").getAsString());
                        if (json.has("score2")) score2Label.setText(json.get("score2").getAsString());
                        if (json.has("player1")) player1Label.setText(json.get("player1").getAsString());
                        if (json.has("player2")) player2Label.setText(json.get("player2").getAsString());

                        String status = json.has("status") ? json.get("status").getAsString() : "ONGOING";
                        
                        if ("COMPLETED".equalsIgnoreCase(status) || "ENDED".equalsIgnoreCase(status)) {
                            stopPolling("Partita Terminata!");
                        } else {
                            statusMessageLabel.setText("Partita in corso...");
                        }
                    } catch (Exception ex) {
                        System.err.println("Polling parsing error: " + ex.getMessage());
                    }
                });
            })
            .exceptionally(ex -> {
                // Se l'endpoint non è ancora pronto o fallisce, usiamo un fallback locale (Mock) per mostrare l'UI in azione
                Platform.runLater(this::mockPollingStep);
                return null;
            });
    }

    // MOCK: Genera numeri finti in assenza di backend reale per il Live Tracking
    private int mockScore1 = 0;
    private int mockScore2 = 0;
    private int pollCount = 0;

    private void mockPollingStep() {
        pollCount++;
        if (Math.random() > 0.5) mockScore1++;
        else mockScore2++;

        score1Label.setText(String.valueOf(mockScore1));
        score2Label.setText(String.valueOf(mockScore2));
        statusMessageLabel.setText("Partita in corso... (Simulata offline)");

        if (pollCount > 5) {
            stopPolling("Partita Terminata!");
        }
    }

    private void stopPolling(String finalMessage) {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }
        liveIndicator.setText("⭕ FINITO");
        liveIndicator.setTextFill(javafx.scene.paint.Color.GRAY);
        liveIndicator.setVisible(true); // Stop blinking

        statusMessageLabel.setText(finalMessage);
        progressIndicator.setVisible(false);
        closeButton.setDisable(false);

        if (onMatchEndedCallback != null) {
            onMatchEndedCallback.run();
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
