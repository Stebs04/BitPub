package com.bitpub.javafx.controllers;

import com.bitpub.core.UIState;
import com.bitpub.model.LeaderboardEntryModel;
import com.bitpub.viewmodels.AdvancedStatsViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdvancedStatsController {

    @FXML private ComboBox<String> viewComboBox;
    @FXML private TableView<LeaderboardEntryModel> statsTable;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageInfoLabel;
    
    // Assume columns exist in FXML (they aren't mapped here but they are in FXML)

    private final AdvancedStatsViewModel viewModel;

    // Injected via DIContainer
    public AdvancedStatsController(AdvancedStatsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        viewComboBox.getItems().addAll("Leaderboard Globale", "Leaderboard Giochi");
        viewComboBox.getSelectionModel().selectFirst();
        
        viewComboBox.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            viewModel.setViewIndex(newVal.intValue());
            viewModel.refresh();
        });

        statsTable.setItems(viewModel.getTableData());
        
        pageInfoLabel.textProperty().bind(viewModel.pageInfoProperty());

        viewModel.currentPageProperty().addListener((obs, oldVal, newVal) -> updateButtons());
        viewModel.totalPagesProperty().addListener((obs, oldVal, newVal) -> updateButtons());
        
        viewModel.getActionState().statusProperty().addListener((obs, oldVal, newVal) -> {
            boolean isLoading = (newVal == UIState.Status.LOADING);
            statsTable.setDisable(isLoading);
        });

        updateButtons();
        viewModel.refresh();
    }

    @FXML
    public void handleRefresh() {
        viewModel.refresh();
    }

    @FXML
    public void handlePrevPage() {
        viewModel.prevPage();
    }

    @FXML
    public void handleNextPage() {
        viewModel.nextPage();
    }

    private void updateButtons() {
        prevButton.setDisable(viewModel.currentPageProperty().get() == 0);
        nextButton.setDisable(viewModel.currentPageProperty().get() >= viewModel.totalPagesProperty().get() - 1);
    }
}
