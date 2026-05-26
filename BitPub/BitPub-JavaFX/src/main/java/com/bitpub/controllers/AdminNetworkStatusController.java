package com.bitpub.controllers;

import com.bitpub.core.UIState;
import com.bitpub.models.EdgeStatus;
import com.bitpub.viewmodels.AdminNetworkViewModel;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Controller dedicato al monitoraggio dello stato di salute della rete degli Edge Nodes.
 * Pattern: MVVM
 */
public class AdminNetworkStatusController {

    @FXML private FlowPane venuesContainer;
    @FXML private Button refreshButton;

    private final AdminNetworkViewModel viewModel;
    private Timeline networkPollingTimeline;

    public AdminNetworkStatusController(AdminNetworkViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        networkPollingTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> handleRefresh()));
        networkPollingTimeline.setCycleCount(Animation.INDEFINITE);
        networkPollingTimeline.play();

        setupBindings();
        handleRefresh();
    }

    private void setupBindings() {
        viewModel.stateProperty().addListener((obs, oldState, newState) -> {
            Platform.runLater(() -> refreshButton.setDisable(newState == UIState.LOADING));
        });

        viewModel.getStatuses().addListener((ListChangeListener<EdgeStatus>) c -> {
            Platform.runLater(() -> {
                venuesContainer.getChildren().clear();
                for (EdgeStatus status : viewModel.getStatuses()) {
                    venuesContainer.getChildren().add(createVenueCard(status));
                }
            });
        });
    }

    @FXML
    public void handleRefresh() {
        viewModel.refreshStatus();
    }

    private VBox createVenueCard(EdgeStatus status) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label nameLabel = new Label(status.getVenueName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox statusIndicator = new HBox(8);
        statusIndicator.setAlignment(Pos.CENTER);
        Circle dot = new Circle(5);
        Label statusText = new Label(status.getStatus());

        if ("ONLINE".equalsIgnoreCase(status.getStatus())) {
            dot.setFill(Color.GREEN);
            statusText.setTextFill(Color.GREEN);
        } else {
            dot.setFill(Color.RED);
            statusText.setTextFill(Color.RED);
        }

        statusIndicator.getChildren().addAll(dot, statusText);
        
        Label lastSeen = new Label("Visto: " + status.getLastSeen());
        lastSeen.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

        card.getChildren().addAll(nameLabel, statusIndicator, lastSeen);
        return card;
    }

    public void stopPolling() {
        if (networkPollingTimeline != null) {
            networkPollingTimeline.stop();
        }
    }
}
