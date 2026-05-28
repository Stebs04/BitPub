package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.model.Game;
import com.bitpub.model.LeaderboardEntryDto;
import com.bitpub.model.MatchResult;
import com.bitpub.network.SessionManager;
import com.bitpub.services.GameNetworkService;
import com.bitpub.services.StatsNetworkService;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.bitpub.utils.JsonManager;
import com.bitpub.model.PageResponse;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PlayerDashboardController {

    private final GameNetworkService gameService;
    private final StatsNetworkService statsService;
    private final Gson gson = JsonManager.getGson();

    // -- Tabelle e Colonne (Storico) --
    @FXML private TableView<MatchResult> matchHistoryTable;
    @FXML private TableColumn<MatchResult, String> colDate;
    @FXML private TableColumn<MatchResult, String> colGame;
    @FXML private TableColumn<MatchResult, String> colOpponent;
    @FXML private TableColumn<MatchResult, String> colResult;

    // -- Tabelle e Colonne (Leaderboard) --
    @FXML private TableView<LeaderboardEntryDto> leaderboardTable;
    @FXML private TableColumn<LeaderboardEntryDto, String> colRank;
    @FXML private TableColumn<LeaderboardEntryDto, String> colPlayer;
    @FXML private TableColumn<LeaderboardEntryDto, Integer> colMatches;
    @FXML private TableColumn<LeaderboardEntryDto, Integer> colWins;
    @FXML private TableColumn<LeaderboardEntryDto, Integer> colElo;

    // -- Tabelle e Colonne (Giochi) --
    @FXML private TableView<Game> gamesTable;
    @FXML private TableColumn<Game, String> colGameName;
    @FXML private TableColumn<Game, String> colGenre;
    @FXML private TableColumn<Game, String> colDescription;

    public PlayerDashboardController(GameNetworkService gameService, StatsNetworkService statsService) {
        this.gameService = gameService;
        this.statsService = statsService;
    }

    @FXML
    public void initialize() {
        setupTables();
        loadData();
    }

    private void setupTables() {
        // Storico
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getPlayedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getPlayedAt().format(formatter));
            }
            return new SimpleStringProperty("");
        });
        colGame.setCellValueFactory(new PropertyValueFactory<>("gameName"));
        
        // Estrai l'avversario e l'esito basandosi sull'username loggato.
        String myUsername = SessionManager.getInstance().getUsername();
        colOpponent.setCellValueFactory(cellData -> {
            MatchResult match = cellData.getValue();
            String op = match.getPlayer1Username().equals(myUsername) ? match.getPlayer2Username() : match.getPlayer1Username();
            return new SimpleStringProperty(op != null ? op : "N/A");
        });
        colResult.setCellValueFactory(cellData -> {
            MatchResult match = cellData.getValue();
            // In un sistema reale, bisognerebbe verificare con gli ID. Per semplicità simuliamo:
            // Assumiamo che se winnerId == null è un pareggio, se coincide col mio userId ho vinto.
            return new SimpleStringProperty("Da Definire");
        });

        // Leaderboard
        colRank.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(leaderboardTable.getItems().indexOf(cellData.getValue()) + 1)));
        colPlayer.setCellValueFactory(new PropertyValueFactory<>("username"));
        colMatches.setCellValueFactory(new PropertyValueFactory<>("totalMatches"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("totalWins"));
        colElo.setCellValueFactory(new PropertyValueFactory<>("eloScore"));

        // Giochi
        colGameName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colGenre.setCellValueFactory(new PropertyValueFactory<>("genre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    private void loadData() {
        // 1. Carica Leaderboard Globale
        statsService.getGlobalLeaderboard().thenAccept(json -> {
            Type type = new TypeToken<PageResponse<LeaderboardEntryDto>>(){}.getType();
            PageResponse<LeaderboardEntryDto> page = gson.fromJson(json, type);
            Platform.runLater(() -> leaderboardTable.setItems(FXCollections.observableArrayList(page.getContent())));
        }).exceptionally(e -> {
            showError("Errore Leaderboard", "Impossibile recuperare la classifica.");
            return null;
        });

        // 2. Carica Giochi
        gameService.getGames().thenAccept(json -> {
            Type type = new TypeToken<PageResponse<Game>>(){}.getType();
            PageResponse<Game> page = gson.fromJson(json, type);
            Platform.runLater(() -> gamesTable.setItems(FXCollections.observableArrayList(page.getContent())));
        }).exceptionally(e -> {
            showError("Errore Giochi", "Impossibile recuperare il catalogo giochi.");
            return null;
        });
        
        // 3. Carica Storico (richiede UUID utente, che andrebbe recuperato dal jwt. Per ora saltiamo o mockiamo)
        // statsService.getMatchHistory(userId)...
    }

    private void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(msg);
            alert.show();
        });
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
