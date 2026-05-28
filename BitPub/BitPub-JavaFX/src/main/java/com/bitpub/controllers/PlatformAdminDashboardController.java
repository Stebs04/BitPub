package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.services.PlatformAdminService;
import com.bitpub.services.StatsNetworkService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class PlatformAdminDashboardController {

    private final PlatformAdminService platformService;
    private final StatsNetworkService statsService;

    public PlatformAdminDashboardController(PlatformAdminService platformService, StatsNetworkService statsService) {
        this.platformService = platformService;
        this.statsService = statsService;
    }

    @FXML
    public void initialize() {
        System.out.println("Platform Admin Dashboard Initialized");
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
