package com.bitpub.viewmodels;

import com.bitpub.core.DialogService;
import com.bitpub.core.NavigationManager;
import com.bitpub.core.UIState;
import com.bitpub.services.AuthService;
import com.bitpub.services.DashboardService;
import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DashboardViewModel {

    private final DashboardService dashboardService;
    private final AuthService authService;
    private final NavigationManager navigationManager;
    private final DialogService dialogService;

    private final StringProperty credit = new SimpleStringProperty("€ 0.00");
    private final UIState<Void> actionState = new UIState<>();

    public DashboardViewModel(DashboardService dashboardService, AuthService authService, 
                              NavigationManager navigationManager, DialogService dialogService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
        this.navigationManager = navigationManager;
        this.dialogService = dialogService;
    }

    public StringProperty creditProperty() { return credit; }
    public UIState<Void> getActionState() { return actionState; }

    public void loadUserProfile() {
        actionState.setLoading();
        dashboardService.getUserProfileAsync()
            .thenAccept(userData -> {
                String creditValue = userData.has("credit") ? userData.get("credit").getAsString() : "0.00";
                javafx.application.Platform.runLater(() -> credit.set("€ " + creditValue));
                actionState.setIdle();
            })
            .exceptionally(ex -> {
                javafx.application.Platform.runLater(() -> credit.set("Errore dati"));
                actionState.setError("Impossibile caricare il profilo.");
                return null;
            });
    }

    public void startFoosballSession() {
        actionState.setLoading();
        dashboardService.startFoosballSessionAsync(1)
            .thenAccept(v -> {
                actionState.setSuccess(null);
                navigationManager.navigateTo("/CalciobalillaUtenteView.fxml", "BitPub - Calciobalilla");
            })
            .exceptionally(ex -> {
                if (ex.getMessage().contains("409")) {
                    recoverSession();
                } else {
                    actionState.setError("Impossibile avviare la sessione: " + ex.getMessage());
                    dialogService.showError("Attenzione", "Impossibile avviare la sessione.");
                }
                return null;
            });
    }

    private void recoverSession() {
        dashboardService.recoverActiveFoosballSessionAsync()
            .thenAccept(v -> {
                actionState.setSuccess(null);
                navigationManager.navigateTo("/CalciobalillaUtenteView.fxml", "BitPub - Calciobalilla");
            })
            .exceptionally(ex -> {
                actionState.setError("Sessione attiva non trovata.");
                dialogService.showError("Errore", "Sessione attiva non trovata.");
                return null;
            });
    }

    public void handleDartsClick() {
        dialogService.showInformation("Info", "Coming soon...");
    }

    public void handleBilliardsClick() {
        dialogService.showInformation("Info", "Coming soon...");
    }

    public void logout() {
        authService.logout();
        com.bitpub.Main.eseguiLogout();
    }
}
