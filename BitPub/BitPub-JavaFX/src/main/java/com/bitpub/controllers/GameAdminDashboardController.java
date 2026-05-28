package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.services.GameNetworkService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class GameAdminDashboardController {

    private final GameNetworkService gameService;

    public GameAdminDashboardController(GameNetworkService gameService) {
        this.gameService = gameService;
    }

    @FXML
    public void initialize() {
        System.out.println("Game Admin Dashboard Initialized");
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
