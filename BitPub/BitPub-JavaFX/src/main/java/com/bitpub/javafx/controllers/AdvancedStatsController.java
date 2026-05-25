package com.bitpub.javafx.controllers;

import com.bitpub.javafx.model.LeaderboardEntryModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;

public class AdvancedStatsController {

    @FXML private ComboBox<String> viewComboBox;
    @FXML private TableView<LeaderboardEntryModel> statsTable;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageInfoLabel;

    private int currentPage = 0;
    private int totalPages = 1;

    private ObservableList<LeaderboardEntryModel> tableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        viewComboBox.getItems().addAll("Leaderboard Globale", "Leaderboard Giochi");
        viewComboBox.getSelectionModel().selectFirst();
        statsTable.setItems(tableData);
        updatePaginationUI();
    }

    @FXML
    public void handleRefresh() {
        currentPage = 0;
        loadData();
    }

    @FXML
    public void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadData();
        }
    }

    @FXML
    public void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadData();
        }
    }

    private void loadData() {
        String endpoint = viewComboBox.getSelectionModel().getSelectedIndex() == 0 
            ? "/api/v1/stats/leaderboard/global?page=" + currentPage + "&size=20"
            : "/api/v1/stats/leaderboard/game/some-game-id?page=" + currentPage + "&size=20"; // Necessita selezione gioco reale

        try {
            com.bitpub.javafx.network.RestClient.get(endpoint, String.class)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        // Deserializza PagedResponseDto e aggiorna tableData
                        // Questo dipenderà dalla libreria JSON usata (es. Jackson/Gson)
                        // tableData.setAll(parsedList);
                        updatePaginationUI();
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Errore nel caricamento statistiche: " + ex.getMessage());
                        alert.show();
                    });
                    return null;
                });
        } catch (Exception e) {
             e.printStackTrace();
        }
    }

    private void updatePaginationUI() {
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= totalPages - 1);
        pageInfoLabel.setText("Pagina " + (currentPage + 1) + " di " + Math.max(1, totalPages));
    }
}
