package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.services.GameNetworkService;
import com.bitpub.services.StatsNetworkService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class PlayerDashboardController {

    private final GameNetworkService gameService;
    private final StatsNetworkService statsService;

    public PlayerDashboardController(GameNetworkService gameService, StatsNetworkService statsService) {
        this.gameService = gameService;
        this.statsService = statsService;
    }

    @FXML
    public void initialize() {
        System.out.println("Player Dashboard Initialized");
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
