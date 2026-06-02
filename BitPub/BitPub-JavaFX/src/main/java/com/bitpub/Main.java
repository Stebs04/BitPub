package com.bitpub;

import com.bitpub.core.DIContainer;
import com.bitpub.core.DialogService;
import com.bitpub.core.NavigationManager;
import com.bitpub.network.RestClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Classe entry-point dell'applicazione client BitPub basata sul framework JavaFX.
 * Agisce come orchestratore globale per il ciclo di vita dell'intero applicativo.
 *
 * @author Stefano Bellan (Refactoring)
 */
public class Main extends Application {

    private static NavigationManager navigationManager;
    private static DIContainer diContainer;

    @Override
    public void start(Stage stage) {
        // 1. Inizializzazione DI Container
        diContainer = DIContainer.getInstance();
        setupDependencies(diContainer);

        // 2. Inizializzazione Navigation Manager
        navigationManager = new NavigationManager(stage, diContainer);
        diContainer.registerSingleton(NavigationManager.class, navigationManager);

        // Iniezione difensiva dell'asset grafico per l'icona dell'applicazione
        try {
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icon.png"))));
        } catch (Exception e) {
            System.out.println("[Main] Icona non trovata, procedo con quella di default.");
        }

        // Pre-riscaldamento del pool di connessioni HTTP
        RestClient.getInstance();

        // Trasferimento del controllo al modulo di autenticazione per l'inizio del flusso utente
        navigationManager.navigateTo("/LoginView.fxml", "BitPub - Login");
    }
    
    private void setupDependencies(DIContainer container) {
        // Registrazione Servizi Globali
        container.registerSingleton(DialogService.class, new DialogService());
        
        com.bitpub.services.AuthService authService = new com.bitpub.services.AuthService();
        container.registerSingleton(com.bitpub.services.AuthService.class, authService);
        
        container.registerSingleton(com.bitpub.services.GameNetworkService.class, new com.bitpub.services.GameNetworkService());
        container.registerSingleton(com.bitpub.services.DeviceNetworkService.class, new com.bitpub.services.DeviceNetworkService());
        container.registerSingleton(com.bitpub.services.StatsNetworkService.class, new com.bitpub.services.StatsNetworkService());
        container.registerSingleton(com.bitpub.services.TournamentNetworkService.class, new com.bitpub.services.TournamentNetworkService());
        container.registerSingleton(com.bitpub.services.PlatformAdminService.class, new com.bitpub.services.PlatformAdminService());

        // Registrazione ViewModel (Login)
        container.registerFactory(com.bitpub.viewmodels.LoginViewModel.class, () -> 
            new com.bitpub.viewmodels.LoginViewModel(
                container.resolve(com.bitpub.services.AuthService.class), 
                container.resolve(NavigationManager.class)
            )
        );

        // Registrazione Controller
        container.registerFactory(com.bitpub.controllers.LoginController.class, () -> 
            new com.bitpub.controllers.LoginController(container.resolve(com.bitpub.viewmodels.LoginViewModel.class))
        );
        
        container.registerFactory(com.bitpub.controllers.PlayerDashboardController.class, () -> 
            new com.bitpub.controllers.PlayerDashboardController(
                container.resolve(com.bitpub.services.GameNetworkService.class),
                container.resolve(com.bitpub.services.StatsNetworkService.class)
            )
        );
        
        container.registerFactory(com.bitpub.controllers.LocalAdminDashboardController.class, () -> 
            new com.bitpub.controllers.LocalAdminDashboardController(
                container.resolve(com.bitpub.services.DeviceNetworkService.class),
                container.resolve(com.bitpub.services.GameNetworkService.class)
            )
        );
        

        container.registerFactory(com.bitpub.controllers.PlatformAdminDashboardController.class, () -> 
            new com.bitpub.controllers.PlatformAdminDashboardController(
                container.resolve(com.bitpub.services.PlatformAdminService.class),
                container.resolve(com.bitpub.services.StatsNetworkService.class)
            )
        );
    }

    /**
     * Motore di routing globale (Legacy, da migrare a NavigationManager)
     */
    @Deprecated
    public static void navigaVerso(String fxmlPath, String titolo) {
        if (navigationManager != null) {
            navigationManager.navigateTo(fxmlPath, titolo);
        }
    }

    /**
     * Innesca la transizione di ripristino post-autenticazione.
     */
    public static void eseguiLogout() {
        System.out.println("[Main] Logout richiesto, reset della scena.");
        if (navigationManager != null) {
            navigationManager.clearAndNavigateToLogin();
        }
    }

    /**
     * Reindirizza l'utente alla dashboard corretta in base al suo ruolo
     */
    public static void redirectDopoLogin() {
        String rawRole = com.bitpub.network.SessionManager.getInstance().getUserRole();
        if (rawRole == null) {
            eseguiLogout();
            return;
        }
        
        String role = rawRole.replace("ROLE_", "").toUpperCase();
        
        switch (role) {
            case "LOCAL_ADMIN":
                navigaVerso("/LocalAdminDashboardView.fxml", "BitPub - Local Admin Dashboard");
                break;

            case "PLATFORM_ADMIN":
                navigaVerso("/PlatformAdminDashboardView.fxml", "BitPub - Platform Admin Dashboard");
                break;
            case "PLAYER":
            default:
                navigaVerso("/PlayerDashboardView.fxml", "BitPub - Player Dashboard");
                break;
        }
    }

    @Override
    public void stop() {
        System.out.println("[Main] Spegnimento in corso... Pulizia risorse.");
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}