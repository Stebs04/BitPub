package com.bitpub.viewmodels;

import com.bitpub.core.DialogService;
import com.bitpub.core.UIState;
import com.bitpub.models.EdgeStatus;
import com.bitpub.services.AdminNetworkService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AdminNetworkViewModel {

    private final AdminNetworkService service;
    private final DialogService dialogService;

    private final ObservableList<EdgeStatus> statuses = FXCollections.observableArrayList();
    private final ObjectProperty<UIState.Status> state = new SimpleObjectProperty<>(UIState.Status.IDLE);

    public AdminNetworkViewModel(AdminNetworkService service, DialogService dialogService) {
        this.service = service;
        this.dialogService = dialogService;
    }

    public ObservableList<EdgeStatus> getStatuses() {
        return statuses;
    }

    public ObjectProperty<UIState.Status> stateProperty() {
        return state;
    }

    public void refreshStatus() {
        state.set(UIState.Status.LOADING);
        service.getNetworkStatus()
            .thenAccept(lista -> Platform.runLater(() -> {
                statuses.setAll(lista);
                state.set(UIState.Status.SUCCESS);
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    state.set(UIState.Status.ERROR);
                    System.err.println("[AdminNetworkViewModel] Errore caricamento status: " + ex.getMessage());
                });
                return null;
            });
    }
}
