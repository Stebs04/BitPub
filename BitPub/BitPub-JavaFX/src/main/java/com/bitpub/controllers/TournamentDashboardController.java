package com.bitpub.controllers;

import com.bitpub.model.TournamentModel;
import com.bitpub.network.RestClient;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class TournamentDashboardController {

    @FXML private TableView<TournamentModel> tournamentsTable;
    @FXML private TableColumn<TournamentModel, String> colName;
    @FXML private TableColumn<TournamentModel, String> colFormat;
    @FXML private TableColumn<TournamentModel, String> colStatus;
    @FXML private TableColumn<TournamentModel, String> colStartDate;

    @FXML private Button btnRegisterTeam;
    @FXML private Button btnGenerateBracket;
    @FXML private Button btnViewTournament;

    private ObservableList<TournamentModel> tournamentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colFormat.setCellValueFactory(new PropertyValueFactory<>("format"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        tournamentsTable.setItems(tournamentList);

        tournamentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            btnRegisterTeam.setDisable(!hasSelection || !"REGISTRATION".equals(newSelection.getStatus()));
            btnGenerateBracket.setDisable(!hasSelection || !"REGISTRATION".equals(newSelection.getStatus()));
            btnViewTournament.setDisable(!hasSelection);
        });

        loadTournaments();
    }

    @FXML
    private void handleRefresh() {
        loadTournaments();
    }

    private void loadTournaments() {
        new Thread(() -> {
            try {
                String response = RestClient.get("/tournaments");
                Type listType = new TypeToken<List<TournamentModel>>(){}.getType();
                List<TournamentModel> list = RestClient.getGson().fromJson(response, listType);
                Platform.runLater(() -> {
                    tournamentList.setAll(list);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleNewTournament() {
        // Mostrerebbe un dialog per creare torneo, per ora mock
        System.out.println("Nuovo torneo cliccato");
    }

    @FXML
    private void handleRegisterTeam() {
        // Mostrerebbe dialog iscrizione team
        System.out.println("Iscrivi team cliccato");
    }

    @FXML
    private void handleGenerateBracket() {
        TournamentModel selected = tournamentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        new Thread(() -> {
            try {
                RestClient.post("/tournaments/" + selected.getId() + "/bracket/generate", "{}");
                Platform.runLater(this::loadTournaments);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleViewTournament() {
        TournamentModel selected = tournamentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            String viewFile = "SINGLE_ELIMINATION".equals(selected.getFormat()) ? "/BracketView.fxml" : "/LeaderboardView.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewFile));
            Parent root = loader.load();

            if ("SINGLE_ELIMINATION".equals(selected.getFormat())) {
                BracketUIController controller = loader.getController();
                controller.setTournament(selected);
            } else {
                LeaderboardUIController controller = loader.getController();
                controller.setTournament(selected);
            }

            Stage stage = new Stage();
            stage.setTitle("Torneo: " + selected.getName());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1000, 700));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
