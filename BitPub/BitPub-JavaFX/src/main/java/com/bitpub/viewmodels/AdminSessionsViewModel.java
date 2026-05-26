package com.bitpub.viewmodels;

import com.bitpub.core.DialogService;
import com.bitpub.core.UIState;
import com.bitpub.services.AdminSessionsService;
import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;

public class AdminSessionsViewModel {
    private final AdminSessionsService sessionsService;
    private final DialogService dialogService;

    private final StringProperty edgeStatus = new SimpleStringProperty("Edge: CARICAMENTO...");
    private final ObservableList<JsonObject> sessions = FXCollections.observableArrayList();
    private final UIState<Void> actionState = new UIState<>();

    public AdminSessionsViewModel(AdminSessionsService sessionsService, DialogService dialogService) {
        this.sessionsService = sessionsService;
        this.dialogService = dialogService;
    }

    public StringProperty edgeStatusProperty() { return edgeStatus; }
    public ObservableList<JsonObject> getSessions() { return sessions; }
    public UIState<Void> getActionState() { return actionState; }

    public void pollEdgeStatus() {
        sessionsService.getEdgeStatus()
            .thenAccept(status -> {
                Platform.runLater(() -> {
                    if ("ONLINE".equals(status)) {
                        edgeStatus.set("Edge: ONLINE");
                    } else {
                        edgeStatus.set("Edge: OFFLINE");
                    }
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> edgeStatus.set("Edge: ERRORE CONNESSIONE"));
                return null;
            });
    }

    public void loadSessions() {
        actionState.setLoading();
        sessionsService.getActiveSessions()
            .thenAccept(list -> {
                Platform.runLater(() -> {
                    sessions.setAll(list);
                    actionState.setSuccess(null);
                });
            })
            .exceptionally(ex -> {
                actionState.setError("Impossibile caricare le sessioni attive: " + ex.getMessage());
                return null;
            });
    }

    public void forceStopSession(JsonObject session) {
        String sessionId = session.has("id") ? session.get("id").getAsString() : "Sconosciuto";
        String forceStopUrl = null;
        if (session.has("_links") && session.getAsJsonObject("_links").has("force-stop")) {
            forceStopUrl = session.getAsJsonObject("_links").getAsJsonObject("force-stop").get("href").getAsString();
        }

        sessionsService.forceStopSession(sessionId, forceStopUrl)
            .thenAccept(v -> {
                Platform.runLater(() -> {
                    dialogService.showInformation("Comando Inviato", "La sessione è stata interrotta forzatamente.");
                    loadSessions();
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> dialogService.showError("Errore API", "Errore durante l'interruzione: " + ex.getMessage()));
                return null;
            });
    }
}
