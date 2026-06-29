package com.bitpub.core;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Handles navigation between different views in the application.
 */
public class NavigationManager {
    private final Stage primaryStage;
    private final DIContainer diContainer;

    public NavigationManager(Stage primaryStage, DIContainer diContainer) {
        this.primaryStage = primaryStage;
        this.diContainer = diContainer;
    }

    public void navigateTo(String fxmlPath, String title) {
        Platform.runLater(() -> {
            try {
                URL resource = getClass().getResource(fxmlPath);
                if (resource == null) {
                    System.err.println("[Navigation] Risorsa non trovata: " + fxmlPath);
                    return;
                }

                FXMLLoader loader = new FXMLLoader(resource);
                // Use DI container for controllers
                loader.setControllerFactory(diContainer::resolve);

                Parent root = loader.load();
                Scene scene = new Scene(root);
                
                URL css = getClass().getResource("/style.css");
                if (css != null) {
                    scene.getStylesheets().add(css.toExternalForm());
                }

                if (title != null) {
                    primaryStage.setTitle(title);
                }
                
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
                primaryStage.show();

                System.out.println("[Navigation] Passaggio a: " + fxmlPath);
            } catch (IOException e) {
                System.err.println("[Navigation] Errore critico nel caricamento della vista: " + fxmlPath);
                e.printStackTrace();
            }
        });
    }

    public void clearAndNavigateToLogin() {
        navigateTo("/LoginView.fxml", "BitPub - Login");
    }
}
