package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.services.PlatformAdminService;
import com.bitpub.services.StatsNetworkService;
import com.bitpub.utils.JsonManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.Map;
import java.util.UUID;

public class PlatformAdminDashboardController {

    private final PlatformAdminService platformService;
    private final StatsNetworkService statsService;
    private final Gson gson = JsonManager.getGson();

    @FXML private TextArea statsArea;
    @FXML private TextField player1Field;
    @FXML private TextField player2Field;
    @FXML private TextField gameIdField;
    @FXML private TextField score1Field;
    @FXML private TextField score2Field;
    @FXML private Label statusLabel;

    public PlatformAdminDashboardController(PlatformAdminService platformService, StatsNetworkService statsService) {
        this.platformService = platformService;
        this.statsService = statsService;
    }

    @FXML
    public void initialize() {
        loadStats();
    }

    @FXML
    public void loadStats() {
        statsService.getStats().thenAccept(json -> {
            Platform.runLater(() -> {
                // Formatta il JSON per renderlo leggibile
                try {
                    JsonObject jsonObj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                    statsArea.setText(gson.toJson(jsonObj));
                } catch(Exception e) {
                    statsArea.setText(json);
                }
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> statsArea.setText("Impossibile recuperare le statistiche globali:\n" + e.getMessage()));
            return null;
        });
    }

    @FXML
    public void submitMatch(ActionEvent event) {
        try {
            UUID p1 = UUID.fromString(player1Field.getText().trim());
            UUID p2 = UUID.fromString(player2Field.getText().trim());
            UUID gameId = UUID.fromString(gameIdField.getText().trim());
            int score1 = Integer.parseInt(score1Field.getText().trim());
            int score2 = Integer.parseInt(score2Field.getText().trim());

            UUID winnerId = score1 > score2 ? p1 : (score2 > score1 ? p2 : null);

            Map<String, Object> payload = Map.of(
                "gameId", gameId,
                "player1Id", p1,
                "player2Id", p2,
                "scorePlayer1", score1,
                "scorePlayer2", score2,
                "winnerId", winnerId != null ? winnerId.toString() : ""
            );

            statsService.createMatch(payload).thenAccept(res -> {
                Platform.runLater(() -> {
                    statusLabel.setText("Match registrato con successo!");
                    statusLabel.setStyle("-fx-text-fill: green;");
                    player1Field.clear(); player2Field.clear(); gameIdField.clear();
                    score1Field.clear(); score2Field.clear();
                    loadStats(); // Aggiorna metriche
                });
            }).exceptionally(e -> {
                Platform.runLater(() -> {
                    statusLabel.setText("Errore dal server: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                });
                return null;
            });

        } catch (Exception e) {
            statusLabel.setText("Errore di validazione campi (controlla gli UUID o i numeri).");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
