package com.bitpub.controllers;

import com.bitpub.core.UIState;
import com.bitpub.models.SystemLog;
import com.bitpub.viewmodels.AdminLogsViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller dedicato al monitoraggio e filtraggio dei log di sistema all'interno della dashboard amministrativa.
 * Pattern: MVVM
 */
public class AdminLogsController {

    @FXML private TableView<SystemLog> logsTable;
    @FXML private TableColumn<SystemLog, String> colTimestamp;
    @FXML private TableColumn<SystemLog, String> colLevel;
    @FXML private TableColumn<SystemLog, String> colSource;
    @FXML private TableColumn<SystemLog, String> colMessage;
    
    @FXML private ComboBox<String> filterLevelCombo;

    private final AdminLogsViewModel viewModel;

    public AdminLogsController(AdminLogsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        logsTable.setItems(viewModel.getLogs());

        if (filterLevelCombo.getItems().isEmpty()) {
            filterLevelCombo.setItems(FXCollections.observableArrayList("ALL", "INFO", "WARN", "ERROR", "DEBUG"));
        }
        
        // Binding bi-direzionale tra combobox e viewModel
        filterLevelCombo.valueProperty().bindBidirectional(viewModel.filterLevelProperty());

        viewModel.stateProperty().addListener((obs, oldState, newState) -> {
            Platform.runLater(() -> {
                if (newState == UIState.Status.LOADING) {
                    logsTable.setPlaceholder(new Label("Ricerca log di sistema in corso..."));
                } else if (newState == UIState.Status.ERROR) {
                    logsTable.setPlaceholder(new Label("Errore di rete: Impossibile caricare i log."));
                } else if (newState == UIState.Status.SUCCESS && viewModel.getLogs().isEmpty()) {
                    logsTable.setPlaceholder(new Label("Nessun log trovato per il filtro selezionato."));
                }
            });
        });

        viewModel.loadLogs();
    }

    @FXML
    public void refreshLogs() {
        viewModel.loadLogs();
    }
}
