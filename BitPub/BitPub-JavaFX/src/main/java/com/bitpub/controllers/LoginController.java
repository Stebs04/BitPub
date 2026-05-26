package com.bitpub.controllers;

import com.bitpub.core.UIState;
import com.bitpub.viewmodels.LoginViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller per la gestione della vista di Login (Refactored to MVVM).
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label erroreLabel;
    @FXML private Button loginButton; // Assumendo che ci sia o usando binding sul testo

    private final LoginViewModel viewModel;

    // Injected via DIContainer by FXMLLoader
    public LoginController(LoginViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @FXML
    public void initialize() {
        // Bind bidirezionale/unidirezionale tra UI e ViewModel
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());

        // Ascolta i cambiamenti di stato
        viewModel.getLoginState().statusProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case LOADING:
                    erroreLabel.setText("Accesso in corso...");
                    erroreLabel.setStyle("-fx-text-fill: blue;");
                    setDisableFields(true);
                    break;
                case ERROR:
                    erroreLabel.setText(viewModel.getLoginState().getErrorMessage());
                    erroreLabel.setStyle("-fx-text-fill: red;");
                    setDisableFields(false);
                    break;
                case SUCCESS:
                    erroreLabel.setText("Accesso riuscito!");
                    erroreLabel.setStyle("-fx-text-fill: green;");
                    break;
                default:
                    erroreLabel.setText("");
                    setDisableFields(false);
            }
        });
    }

    private void setDisableFields(boolean disable) {
        usernameField.setDisable(disable);
        passwordField.setDisable(disable);
        // Se c'è un bottone collegato al submit, bisognerebbe disabilitarlo.
    }

    @FXML
    public void handleLogin() {
        viewModel.login();
    }

    @FXML
    private void vaiARegistrazione() {
        viewModel.goToRegistration();
    }
}

