package com.bitpub.controllers;

import com.bitpub.core.DIContainer;
import com.bitpub.core.NavigationManager;
import com.bitpub.services.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;

/**
 * Controller di orchestrazione (Shell) per l'interfaccia di amministrazione.
 * (Refactored to use DIContainer)
 */
public class AdminMainController {

    @FXML private StackPane contentArea;

    private final AuthService authService;
    private final NavigationManager navigationManager;
    private final DIContainer diContainer;

    // Injected via DIContainer
    public AdminMainController(AuthService authService, NavigationManager navigationManager, DIContainer diContainer) {
        this.authService = authService;
        this.navigationManager = navigationManager;
        this.diContainer = diContainer;
    }

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML private void showDashboard() { loadView("/AdminDashboardView.fxml"); }
    @FXML private void showNetworkStatus() { loadView("/AdminNetworkStatus.fxml"); }
    @FXML private void showUsers() { loadView("/AdminUsers.fxml"); }
    @FXML private void showSessions() { loadView("/AdminSessionView.fxml"); }
    @FXML private void showLogs() { loadView("/AdminLogsView.fxml"); }

    private void loadView(String fxmlPath) {
        Platform.runLater(() -> {
            try {
                contentArea.getChildren().clear();
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.setControllerFactory(diContainer::resolve);
                Parent view = loader.load();
                
                contentArea.getChildren().add(view);
                
                System.out.println("[AdminMain] Modulo caricato: " + fxmlPath);
            } catch (IOException e) {
                System.err.println("[AdminMain] Errore caricamento vista: " + fxmlPath);
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleLogout() {
        authService.logout();
        Platform.runLater(() -> {
            System.out.println("[AdminMain] Logout eseguito, ritorno al Login.");
            navigationManager.clearAndNavigateToLogin();
        });
    }
}