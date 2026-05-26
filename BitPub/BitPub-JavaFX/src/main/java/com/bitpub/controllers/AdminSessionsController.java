package com.bitpub.controllers;

import com.bitpub.core.NavigationManager;
import com.bitpub.core.UIState;
import com.bitpub.viewmodels.AdminSessionsViewModel;
import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Optional;

/**
 * Controller per la dashboard amministrativa delle sessioni attive (Refactored to MVVM).
 */
public class AdminSessionsController {

    @FXML private TableView<JsonObject> tableActiveSessions;
    @FXML private TableColumn<JsonObject, String> colSessionId;
    @FXML private TableColumn<JsonObject, String> colGameType;
    @FXML private TableColumn<JsonObject, String> colUser;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private TableColumn<JsonObject, String> colScore;
    @FXML private TableColumn<JsonObject, Void> colActions;
    @FXML private Label lblEdgeStatus;

    private final AdminSessionsViewModel viewModel;
    private final NavigationManager navigationManager;
    private Timeline edgeStatusTimeline;

    public AdminSessionsController(AdminSessionsViewModel viewModel, NavigationManager navigationManager) {
        this.viewModel = viewModel;
        this.navigationManager = navigationManager;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        tableActiveSessions.setItems(viewModel.getSessions());

        lblEdgeStatus.textProperty().bind(viewModel.edgeStatusProperty());
        viewModel.edgeStatusProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.contains("ONLINE")) {
                lblEdgeStatus.setTextFill(Color.web("#198754"));
            } else {
                lblEdgeStatus.setTextFill(Color.web("#dc3545"));
            }
        });

        viewModel.getActionState().statusProperty().addListener((obs, oldState, newState) -> {
            if (newState == UIState.Status.ERROR) {
                // Notificare errore visuale se necessario, il dialog è gestito a parte o si usa una label
            }
        });

        viewModel.loadSessions();

        edgeStatusTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> viewModel.pollEdgeStatus()));
        edgeStatusTimeline.setCycleCount(Animation.INDEFINITE);
        edgeStatusTimeline.play();
    }

    private void setupTableColumns() {
        colSessionId.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("id") ? cellData.getValue().get("id").getAsString() : ""));
            
        colGameType.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("gameType") ? cellData.getValue().get("gameType").getAsString() : ""));
            
        colUser.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("userId") ? cellData.getValue().get("userId").getAsString() : ""));
            
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("status") ? cellData.getValue().get("status").getAsString() : ""));
            
        colScore.setCellValueFactory(cellData -> {
            JsonObject session = cellData.getValue();
            String score = (session.has("scoreBlue") ? session.get("scoreBlue").getAsString() : "0") 
                            + " - " + 
                           (session.has("scoreRed") ? session.get("scoreRed").getAsString() : "0");
            return new SimpleStringProperty(score);
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnForceStop = new Button("Forza Chiusura");
            {
                btnForceStop.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btnForceStop.setOnAction(event -> {
                    JsonObject session = getTableView().getItems().get(getIndex());
                    handleForceStop(session);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnForceStop);
                }
            }
        });
    }

    private void handleForceStop(JsonObject session) {
        String sessionId = session.has("id") ? session.get("id").getAsString() : "Sconosciuto";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma Chiusura Forzata");
        confirm.setHeaderText("Stai per forzare la chiusura della sessione ID: " + sessionId);
        confirm.setContentText("Vuoi procedere? L'Edge Node sbloccherà il tavolo.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            viewModel.forceStopSession(session);
        }
    }

    @FXML
    public void handleRefresh() {
        viewModel.loadSessions();
    }

    @FXML
    void handleBackToDashboard(ActionEvent event) {
        if (edgeStatusTimeline != null) {
            edgeStatusTimeline.stop();
        }
        navigationManager.navigateTo("/AdminDashboardView.fxml", "BitPub - Admin Dashboard");
    }
}