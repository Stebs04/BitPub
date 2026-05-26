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
        
        com.bitpub.services.DashboardService dashboardService = new com.bitpub.services.DashboardService();
        container.registerSingleton(com.bitpub.services.DashboardService.class, dashboardService);

        container.registerSingleton(com.bitpub.services.AdminDashboardService.class, new com.bitpub.services.AdminDashboardService());
        container.registerSingleton(com.bitpub.services.AdminLogsService.class, new com.bitpub.services.AdminLogsService());
        container.registerSingleton(com.bitpub.services.AdminNetworkService.class, new com.bitpub.services.AdminNetworkService());
        container.registerSingleton(com.bitpub.services.AdminSessionsService.class, new com.bitpub.services.AdminSessionsService(RestClient.getInstance()));
        container.registerSingleton(com.bitpub.services.AdminUsersService.class, new com.bitpub.services.AdminUsersService());

        // Registrazione ViewModel
        container.registerFactory(com.bitpub.viewmodels.LoginViewModel.class, () -> 
            new com.bitpub.viewmodels.LoginViewModel(
                container.resolve(com.bitpub.services.AuthService.class), 
                container.resolve(NavigationManager.class)
            )
        );
        
        container.registerFactory(com.bitpub.viewmodels.DashboardViewModel.class, () -> 
            new com.bitpub.viewmodels.DashboardViewModel(
                container.resolve(com.bitpub.services.DashboardService.class), 
                container.resolve(com.bitpub.services.AuthService.class),
                container.resolve(NavigationManager.class),
                container.resolve(DialogService.class)
            )
        );

        container.registerFactory(com.bitpub.viewmodels.AdminDashboardViewModel.class, () -> 
            new com.bitpub.viewmodels.AdminDashboardViewModel(
                container.resolve(com.bitpub.services.AdminDashboardService.class),
                container.resolve(DialogService.class)
            )
        );

        container.registerFactory(com.bitpub.viewmodels.AdminLogsViewModel.class, () -> 
            new com.bitpub.viewmodels.AdminLogsViewModel(
                container.resolve(com.bitpub.services.AdminLogsService.class),
                container.resolve(DialogService.class)
            )
        );

        container.registerFactory(com.bitpub.viewmodels.AdminNetworkViewModel.class, () -> 
            new com.bitpub.viewmodels.AdminNetworkViewModel(
                container.resolve(com.bitpub.services.AdminNetworkService.class),
                container.resolve(DialogService.class)
            )
        );

        container.registerFactory(com.bitpub.viewmodels.AdminUsersViewModel.class, () -> 
            new com.bitpub.viewmodels.AdminUsersViewModel(
                container.resolve(com.bitpub.services.AdminUsersService.class),
                container.resolve(DialogService.class)
            )
        );

        container.registerFactory(com.bitpub.viewmodels.AdvancedStatsViewModel.class, () -> 
            new com.bitpub.viewmodels.AdvancedStatsViewModel(
                container.resolve(com.bitpub.services.StatsService.class),
                container.resolve(DialogService.class)
            )
        );

        // Registrazione Controller
        container.registerFactory(com.bitpub.controllers.LoginController.class, () -> 
            new com.bitpub.controllers.LoginController(container.resolve(com.bitpub.viewmodels.LoginViewModel.class))
        );
        
        container.registerFactory(com.bitpub.controllers.DashboardController.class, () -> 
            new com.bitpub.controllers.DashboardController(container.resolve(com.bitpub.viewmodels.DashboardViewModel.class))
        );
        
        container.registerFactory(com.bitpub.controllers.AdminMainController.class, () -> 
            new com.bitpub.controllers.AdminMainController(
                container.resolve(com.bitpub.services.AuthService.class),
                container.resolve(NavigationManager.class),
                container
            )
        );

        container.registerFactory(com.bitpub.controllers.AdminDashboardController.class, () -> 
            new com.bitpub.controllers.AdminDashboardController(container.resolve(com.bitpub.viewmodels.AdminDashboardViewModel.class))
        );

        container.registerFactory(com.bitpub.controllers.AdminLogsController.class, () -> 
            new com.bitpub.controllers.AdminLogsController(container.resolve(com.bitpub.viewmodels.AdminLogsViewModel.class))
        );

        container.registerFactory(com.bitpub.controllers.AdminNetworkStatusController.class, () -> 
            new com.bitpub.controllers.AdminNetworkStatusController(container.resolve(com.bitpub.viewmodels.AdminNetworkViewModel.class))
        );

        container.registerFactory(com.bitpub.controllers.AdminSessionsController.class, () -> 
            new com.bitpub.controllers.AdminSessionsController(
                container.resolve(com.bitpub.viewmodels.AdminSessionsViewModel.class),
                container.resolve(NavigationManager.class)
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
            case "ADMIN":
                navigaVerso("/AdminMainLayout.fxml", "BitPub - Admin Dashboard");
                break;
            case "GESTORE":
                navigaVerso("/GestoreDashboardView.fxml", "BitPub - Gestore Dashboard");
                break;
            case "UTENTE_BASE":
            default:
                navigaVerso("/DashboardView.fxml", "BitPub - Dashboard Utente");
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
});
    }
}