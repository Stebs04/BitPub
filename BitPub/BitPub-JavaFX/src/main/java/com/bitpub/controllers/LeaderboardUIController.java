package com.bitpub.controllers;

import com.bitpub.model.LeaderboardEntryModel;
import com.bitpub.model.TournamentModel;
import com.bitpub.network.RestClient;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.lang.reflect.Type;
import java.util.List;

public class LeaderboardUIController {

    @FXML private Label lblTournamentName;
    @FXML private TableView<LeaderboardEntryModel> leaderboardTable;
    @FXML private TableColumn<LeaderboardEntryModel, String> colTeam;
    @FXML private TableColumn<LeaderboardEntryModel, Integer> colPoints;
    @FXML private TableColumn<LeaderboardEntryModel, Integer> colWins;
    @FXML private TableColumn<LeaderboardEntryModel, Integer> colDraws;
    @FXML private TableColumn<LeaderboardEntryModel, Integer> colLosses;
    @FXML private TableColumn<LeaderboardEntryModel, Integer> colGoalsFor;
    @FXML private TableColumn<LeaderboardEntryModel, Integer> colGoalsAgainst;

    private TournamentModel tournament;
    private ObservableList<LeaderboardEntryModel> entriesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colTeam.setCellValueFactory(new PropertyValueFactory<>("teamName"));
        colPoints.setCellValueFactory(new PropertyValueFactory<>("points"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        colDraws.setCellValueFactory(new PropertyValueFactory<>("draws"));
        colLosses.setCellValueFactory(new PropertyValueFactory<>("losses"));
        colGoalsFor.setCellValueFactory(new PropertyValueFactory<>("goalsFor"));
        colGoalsAgainst.setCellValueFactory(new PropertyValueFactory<>("goalsAgainst"));

        leaderboardTable.setItems(entriesList);
    }

    public void setTournament(TournamentModel tournament) {
        this.tournament = tournament;
        lblTournamentName.setText("Classifica: " + tournament.getName());
        loadLeaderboard();
    }

    @FXML
    private void loadLeaderboard() {
        if (tournament == null) return;
        new Thread(() -> {
            try {
                String response = RestClient.get("/tournaments/" + tournament.getId() + "/leaderboard");
                Type listType = new TypeToken<List<LeaderboardEntryModel>>(){}.getType();
                List<LeaderboardEntryModel> list = RestClient.getGson().fromJson(response, listType);
                Platform.runLater(() -> entriesList.setAll(list));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
