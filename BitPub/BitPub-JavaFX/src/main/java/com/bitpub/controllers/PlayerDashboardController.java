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

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
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
        startBackgroundPolling();
    }

    private void startBackgroundPolling() {
        // Aggiorna silenziosamente la leaderboard ogni 15 secondi per riflettere le partite giocate da altri
        Timeline backgroundPolling = new Timeline(new KeyFrame(Duration.seconds(15), e -> {
            loadData();
        }));
        backgroundPolling.setCycleCount(Animation.INDEFINITE);
        backgroundPolling.play();
    }

    private void setupTables() {
        // Storico partite
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getPlayedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getPlayedAt().format(formatter));
            }
            return new SimpleStringProperty("");
        });

        // Il gameId non ha un nome: mostriamo l'ID abbreviato come fallback
        colGame.setCellValueFactory(cellData -> {
            UUID gid = cellData.getValue().getGameId();
            return new SimpleStringProperty(gid != null ? gid.toString().substring(0, 8) + "..." : "N/A");
        });

        // Avversario: il campo userId della sessione identifica chi sei tu
        UUID myUserId = SessionManager.getInstance().getUserId();
        colOpponent.setCellValueFactory(cellData -> {
            MatchResult match = cellData.getValue();
            if (myUserId == null) return new SimpleStringProperty("N/A");
            // Se io sono il vincitore, l'avversario è il perdente e viceversa
            boolean iAmWinner = myUserId.equals(match.getWinnerUserId());
            UUID opponentId = iAmWinner ? match.getLoserUserId() : match.getWinnerUserId();
            return new SimpleStringProperty(opponentId != null ? opponentId.toString().substring(0, 8) + "..." : "N/A");
        });

        // Risultato: vinto/perso basato sull'userId della sessione
        colResult.setCellValueFactory(cellData -> {
            MatchResult match = cellData.getValue();
            if (myUserId == null) return new SimpleStringProperty("N/A");
            if (myUserId.equals(match.getWinnerUserId())) {
                return new SimpleStringProperty("✅ Vinto (" + match.getWinnerScore() + " - " + match.getLoserScore() + ")");
            } else {
                return new SimpleStringProperty("❌ Perso (" + match.getLoserScore() + " - " + match.getWinnerScore() + ")");
            }
        });

        // Leaderboard — rank viene già dal backend
        colRank.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getRank())));
        colPlayer.setCellValueFactory(new PropertyValueFactory<>("username"));
        colMatches.setCellValueFactory(new PropertyValueFactory<>("totalMatches"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("wins"));
        // colElo riciclato per mostrare il totalScore (punteggio cumulativo)
        colElo.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getTotalScore()).asObject());

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
            showError("Errore Leaderboard", "Impossibile recuperare la classifica: " + e.getCause().getMessage());
            return null;
        });

        // 2. Carica Giochi
        gameService.getGames().thenAccept(json -> {
            Type type = new TypeToken<PageResponse<Game>>(){}.getType();
            PageResponse<Game> page = gson.fromJson(json, type);
            Platform.runLater(() -> gamesTable.setItems(FXCollections.observableArrayList(page.getContent())));
        }).exceptionally(e -> {
            showError("Errore Giochi", "Impossibile recuperare il catalogo giochi: " + e.getCause().getMessage());
            return null;
        });

        // 3. Carica Storico Partite dell'utente loggato
        UUID myUserId = SessionManager.getInstance().getUserId();
        if (myUserId != null) {
            statsService.getMatchHistory(myUserId).thenAccept(json -> {
                Type type = new TypeToken<PageResponse<MatchResult>>(){}.getType();
                PageResponse<MatchResult> page = gson.fromJson(json, type);
                Platform.runLater(() -> {
                    if (page != null && page.getContent() != null) {
                        matchHistoryTable.setItems(FXCollections.observableArrayList(page.getContent()));
                    }
                });
            }).exceptionally(e -> {
                // Non mostriamo un errore bloccante se lo storico non è disponibile
                System.err.println("[PlayerDashboard] Impossibile caricare storico: " + e.getMessage());
                return null;
            });
        }
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
    public void playSimulatedMatch(ActionEvent event) {
        Game selected = gamesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Attenzione", "Seleziona prima un gioco dalla lista.");
            return;
        }

        String type = selected.getName();
        if (type.equalsIgnoreCase("Calciobalilla")) type = "TABLE_FOOTBALL";
        else if (type.equalsIgnoreCase("Biliardo")) type = "POOL";
        else if (type.equalsIgnoreCase("Freccette")) type = "DARTS";

        final String gameType = type;
        com.bitpub.network.RestClient.getInstance().postAsync(
            com.bitpub.network.RestClient.getInstance().getRootUrl() + "/api/v1/simulators/simulate/" + gameType,
            null, String.class)
            .thenAccept(result -> {
                Platform.runLater(() -> {
                    try {
                        String sessionId = UUID.randomUUID().toString(); // Fallback mock sessionId
                        try {
                            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(result).getAsJsonObject();
                            if (json.has("sessionId")) {
                                sessionId = json.get("sessionId").getAsString();
                            }
                        } catch (Exception ignored) { }

                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/LiveScoreboardView.fxml"));
                        javafx.scene.Parent root = loader.load();
                        
                        LiveScoreboardController controller = loader.getController();
                        // Avvia il controller Live passandogli la callback per ricaricare la dashboard a fine partita
                        controller.initData(selected.getName(), sessionId, this::loadData);

                        javafx.stage.Stage stage = new javafx.stage.Stage();
                        stage.setTitle("Live Match - " + selected.getName());
                        stage.setScene(new javafx.scene.Scene(root));
                        stage.show();
                    } catch (Exception e) {
                        showError("Errore Apertura", "Impossibile caricare il Live Tracker: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            })
            .exceptionally(e -> {
                showError("Errore di avvio", "Impossibile contattare il simulatore: " + e.getMessage());
                return null;
            });
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
