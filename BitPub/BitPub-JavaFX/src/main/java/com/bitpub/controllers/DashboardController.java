package com.bitpub.controllers;

import com.bitpub.core.UIState;
import com.bitpub.viewmodels.DashboardViewModel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controller per la Dashboard Utente principale (Refactored to MVVM).
 * Implementa il binding con DashboardViewModel.
 */
public class DashboardController {

    @FXML private Label lblCredit;
    @FXML private Button btnFoosball;
    @FXML private Button btnDarts;
    @FXML private Button btnBilliards;
    @FXML private Button btnLogout;

    private final DashboardViewModel viewModel;

    // Injected via DIContainer
    public DashboardController(DashboardViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML 
    public void initialize() {
        // Binding del credito
        lblCredit.textProperty().bind(viewModel.creditProperty());

        // Ascolto dello stato per bloccare/sbloccare l'UI
        viewModel.getActionState().statusProperty().addListener((obs, oldState, newState) -> {
            boolean isLoading = newState == UIState.Status.LOADING;
            btnFoosball.setDisable(isLoading);
            btnDarts.setDisable(isLoading);
            btnBilliards.setDisable(isLoading);
            btnLogout.setDisable(isLoading);
            
            if (isLoading) {
                lblCredit.textProperty().unbind();
                lblCredit.setText("Attendere...");
            } else {
                lblCredit.textProperty().bind(viewModel.creditProperty());
            }
        });

        // Caricamento asincrono iniziale
        viewModel.loadUserProfile();
    }

    @FXML 
    void handleFoosballClick(ActionEvent event) {
        viewModel.startFoosballSession();
    }

    @FXML 
    void handleLogout(ActionEvent event) {
        viewModel.logout();
    }

    @FXML 
    void handleDartsClick(ActionEvent event) {
        viewModel.handleDartsClick();
    }

    @FXML 
    void handleBilliardsClick(ActionEvent event) {
        viewModel.handleBilliardsClick();
    }
}