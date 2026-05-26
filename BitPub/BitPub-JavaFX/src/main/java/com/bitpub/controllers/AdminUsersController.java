package com.bitpub.controllers;

import com.bitpub.core.UIState;
import com.bitpub.models.Utente;
import com.bitpub.viewmodels.AdminUsersViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;

/**
 * Controller responsabile della gestione dell'interfaccia di amministrazione degli utenti.
 * (Refactored to MVVM)
 */
public class AdminUsersController {

    @FXML private TextField searchField;
    @FXML private TableView<Utente> usersTable;
    @FXML private TableColumn<Utente, String> colUsername, colEmail, colRole, colStato;
    @FXML private TableColumn<Utente, Double> colCredito;
    
    @FXML private Button toggleRoleButton;
    @FXML private Button toggleStatusButton;

    private final AdminUsersViewModel viewModel;

    // Injected via DIContainer
    public AdminUsersController(AdminUsersViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colCredito.setCellValueFactory(new PropertyValueFactory<>("credito"));
        colStato.setCellValueFactory(new PropertyValueFactory<>("stato"));

        usersTable.setItems(viewModel.getUsers());
        
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean isSelected = (newSelection != null);
            toggleRoleButton.setDisable(!isSelected);
            if (toggleStatusButton != null) toggleStatusButton.setDisable(!isSelected);
        });

        viewModel.getActionState().statusProperty().addListener((obs, oldVal, newVal) -> {
            boolean isLoading = newVal == UIState.Status.LOADING;
            searchField.setDisable(isLoading);
            usersTable.setDisable(isLoading);
            if (!isLoading) {
                // ripristino bottoni in base alla selezione attuale
                boolean isSelected = (usersTable.getSelectionModel().getSelectedItem() != null);
                toggleRoleButton.setDisable(!isSelected);
                if (toggleStatusButton != null) toggleStatusButton.setDisable(!isSelected);
            }
        });

        handleSearch();
    }

    @FXML
    public void handleSearch() {
        viewModel.loadUsers();
    }

    @FXML
    public void handleToggleRole() {
        Utente selezionato = usersTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) return;

        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION, "Vuoi cambiare il ruolo di " + selezionato.getUsername() + "?");
        conferma.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                viewModel.toggleRole(selezionato);
            }
        });
    }

    @FXML
    public void handleToggleStatus() {
        Utente selezionato = usersTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) return;

        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION, "Vuoi cambiare lo stato di " + selezionato.getUsername() + "?");
        conferma.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                viewModel.toggleStatus(selezionato);
            }
        });
    }
}