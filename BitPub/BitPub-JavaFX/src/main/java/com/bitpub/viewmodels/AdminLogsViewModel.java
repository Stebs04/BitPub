package com.bitpub.viewmodels;

import com.bitpub.core.DialogService;
import com.bitpub.core.UIState;
import com.bitpub.models.SystemLog;
import com.bitpub.services.AdminLogsService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AdminLogsViewModel {

    private final AdminLogsService service;
    private final DialogService dialogService;

    private final ObservableList<SystemLog> logs = FXCollections.observableArrayList();
    private final ObjectProperty<UIState.Status> state = new SimpleObjectProperty<>(UIState.Status.IDLE);
    private final StringProperty filterLevel = new SimpleStringProperty("ALL");

    public AdminLogsViewModel(AdminLogsService service, DialogService dialogService) {
        this.service = service;
        this.dialogService = dialogService;
    }

    public ObservableList<SystemLog> getLogs() {
        return logs;
    }

    public ObjectProperty<UIState.Status> stateProperty() {
        return state;
    }
    
    public StringProperty filterLevelProperty() {
        return filterLevel;
    }

    public void loadLogs() {
        state.set(UIState.Status.LOADING);
        service.getLogs(filterLevel.get())
            .thenAccept(lista -> Platform.runLater(() -> {
                logs.setAll(lista);
                state.set(UIState.Status.SUCCESS);
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    logs.clear();
                    state.set(UIState.Status.ERROR);
                    System.err.println("[AdminLogsViewModel] Errore caricamento log: " + ex.getMessage());
                });
                return null;
            });
    }
}
