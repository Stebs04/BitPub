package com.bitpub.viewmodels;

import com.bitpub.core.DialogService;
import com.bitpub.core.UIState;
import com.bitpub.models.Utente;
import com.bitpub.services.AdminUsersService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AdminUsersViewModel {
    private final AdminUsersService usersService;
    private final DialogService dialogService;

    private final StringProperty searchQuery = new SimpleStringProperty("");
    private final ObservableList<Utente> users = FXCollections.observableArrayList();
    private final UIState<Void> actionState = new UIState<>();

    public AdminUsersViewModel(AdminUsersService usersService, DialogService dialogService) {
        this.usersService = usersService;
        this.dialogService = dialogService;
    }

    public StringProperty searchQueryProperty() { return searchQuery; }
    public ObservableList<Utente> getUsers() { return users; }
    public UIState<Void> getActionState() { return actionState; }

    public void loadUsers() {
        actionState.setLoading();
        usersService.getUsers(searchQuery.get())
            .thenAccept(list -> {
                Platform.runLater(() -> {
                    users.setAll(list);
                    actionState.setSuccess(null);
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> dialogService.showError("Errore", "Impossibile recuperare gli utenti: " + ex.getMessage()));
                actionState.setError(ex.getMessage());
                return null;
            });
    }

    public void toggleRole(Utente user) {
        if ("ADMIN".equals(user.getRole())) {
            dialogService.showInformation("Azione Negata", "Non è possibile modificare il ruolo di un Amministratore.");
            return;
        }

        if (user.getLinks() == null || !user.getLinks().containsKey("toggle-role")) {
            dialogService.showError("Errore", "L'operazione 'Cambio Ruolo' non è permessa per questo utente.");
            return;
        }

        String toggleUrl = user.getLinkHref("toggle-role");

        actionState.setLoading();
        usersService.toggleRole(toggleUrl)
            .thenAccept(res -> {
                Platform.runLater(() -> {
                    dialogService.showInformation("Successo", "Ruolo aggiornato correttamente.");
                    loadUsers();
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> dialogService.showError("Errore", "Modifica fallita: " + ex.getMessage()));
                actionState.setError(ex.getMessage());
                return null;
            });
    }

    public void toggleStatus(Utente user) {
        actionState.setLoading();
        usersService.toggleStatus(user.getUsername())
            .thenAccept(res -> {
                Platform.runLater(() -> {
                    dialogService.showInformation("Successo", "Stato aggiornato correttamente.");
                    loadUsers();
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> dialogService.showError("Errore", "Modifica fallita: " + ex.getMessage()));
                actionState.setError(ex.getMessage());
                return null;
            });
    }
}
