package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.model.Game;
import com.bitpub.model.PageResponse;
import com.bitpub.services.GameNetworkService;
import com.bitpub.utils.JsonManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.lang.reflect.Type;

public class GameAdminDashboardController {

    private final GameNetworkService gameService;
    private final Gson gson = JsonManager.getGson();

    @FXML private TableView<Game> gamesTable;
    @FXML private TableColumn<Game, String> colName;
    @FXML private TableColumn<Game, String> colGenre;
    @FXML private TableColumn<Game, String> colDescription;

    @FXML private TextField nameField;
    @FXML private TextField genreField;
    @FXML private TextArea descriptionField;
    @FXML private Label statusLabel;

    public GameAdminDashboardController(GameNetworkService gameService) {
        this.gameService = gameService;
    }

    @FXML
    public void initialize() {
        setupTable();
        loadGames();
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colGenre.setCellValueFactory(new PropertyValueFactory<>("genre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    private void loadGames() {
        gameService.getGames().thenAccept(json -> {
            Type type = new TypeToken<PageResponse<Game>>(){}.getType();
            PageResponse<Game> page = gson.fromJson(json, type);
            Platform.runLater(() -> {
                gamesTable.setItems(FXCollections.observableArrayList(page.getContent()));
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("Impossibile recuperare il catalogo.");
                statusLabel.setStyle("-fx-text-fill: red;");
            });
            return null;
        });
    }

    @FXML
    public void createGame(ActionEvent event) {
        String name = nameField.getText();
        String genre = genreField.getText();
        String desc = descriptionField.getText();

        if (name == null || name.trim().isEmpty() || genre == null || genre.trim().isEmpty()) {
            statusLabel.setText("Nome e Genere sono obbligatori.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        Game newGame = new Game();
        newGame.setName(name.trim());
        newGame.setGenre(genre.trim());
        newGame.setDescription(desc != null ? desc.trim() : "");

        gameService.createGame(newGame).thenAccept(res -> {
            Platform.runLater(() -> {
                statusLabel.setText("Gioco aggiunto con successo!");
                statusLabel.setStyle("-fx-text-fill: green;");
                nameField.clear();
                genreField.clear();
                descriptionField.clear();
                loadGames(); // Refresh table
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                statusLabel.setText("Errore durante la creazione: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
            });
            return null;
        });
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
