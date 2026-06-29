package com.bitpub.viewmodels;

import com.bitpub.core.NavigationManager;
import com.bitpub.core.UIState;
import com.bitpub.services.AuthService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class LoginViewModel {

    private final AuthService authService;
    private final NavigationManager navigationManager;

    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final UIState<Void> loginState = new UIState<>();

    public LoginViewModel(AuthService authService, NavigationManager navigationManager) {
        this.authService = authService;
        this.navigationManager = navigationManager;
    }

    public StringProperty usernameProperty() { return username; }
    public StringProperty passwordProperty() { return password; }
    public UIState<Void> getLoginState() { return loginState; }

    public void login() {
        if (username.get().isEmpty() || password.get().isEmpty()) {
            loginState.setError("Inserisci username e password.");
            return;
        }

        loginState.setLoading();

        authService.loginAsync(username.get(), password.get())
                .thenAccept(v -> {
                    loginState.setSuccess(null);
                    com.bitpub.Main.redirectDopoLogin(); // Still static for now, can be moved to NavigationManager later
                })
                .exceptionally(ex -> {
                    System.err.println("[LOGIN REFACTOR] Errore: " + ex.getMessage());
                    loginState.setError("Impossibile accedere: credenziali errate o server offline.");
                    return null;
                });
    }

    public void goToRegistration() {
        navigationManager.navigateTo("/RegistrazioneView.fxml", "BitPub - Registrazione");
    }
}
