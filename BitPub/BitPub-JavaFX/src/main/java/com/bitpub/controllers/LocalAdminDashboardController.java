package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.services.GameNetworkService;
import com.bitpub.services.StatsNetworkService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class LocalAdminDashboardController {

    private final GameNetworkService gameService;
    private final StatsNetworkService statsService;

    public LocalAdminDashboardController(GameNetworkService gameService, StatsNetworkService statsService) {
        this.gameService = gameService;
        this.statsService = statsService;
    }

    @FXML
    public void initialize() {
        System.out.println("Local Admin Dashboard Initialized");
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
