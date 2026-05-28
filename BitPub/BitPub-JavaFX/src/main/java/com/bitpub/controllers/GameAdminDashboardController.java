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
            });
            return null;
        });
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
