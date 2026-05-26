package com.bitpub.viewmodels;

import com.bitpub.core.DialogService;
import com.bitpub.core.UIState;
import com.bitpub.models.Locale;
import com.bitpub.models.Utente;
import com.bitpub.services.AdminDashboardService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.function.Consumer;

public class AdminDashboardViewModel {

    private final AdminDashboardService service;
    private final DialogService dialogService;

    private final ObservableList<Locale> locali = FXCollections.observableArrayList();
    private final ObjectProperty<UIState> state = new SimpleObjectProperty<>(UIState.IDLE);

    public AdminDashboardViewModel(AdminDashboardService service, DialogService dialogService) {
        this.service = service;
        this.dialogService = dialogService;
    }

    public ObservableList<Locale> getLocali() {
        return locali;
    }

    public ObjectProperty<UIState> stateProperty() {
        return state;
    }

    public void loadLocali() {
        state.set(UIState.LOADING);
        service.getLocali()
            .thenAccept(lista -> Platform.runLater(() -> {
                locali.setAll(lista);
                state.set(UIState.SUCCESS);
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    state.set(UIState.ERROR);
                    dialogService.showError("Errore", "Impossibile caricare i locali: " + ex.getMessage());
                });
                return null;
            });
    }

    public void startNuovoLocale(Consumer<List<Utente>> onGestoriLoaded) {
        state.set(UIState.LOADING);
        service.getGestori()
            .thenAccept(gestori -> Platform.runLater(() -> {
                state.set(UIState.SUCCESS);
                onGestoriLoaded.accept(gestori);
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    state.set(UIState.ERROR);
                    dialogService.showError("Errore", "Impossibile recuperare i gestori.");
                });
                return null;
            });
    }

    public void createLocale(Locale nuovoLocale) {
        state.set(UIState.LOADING);
        service.createLocale(nuovoLocale)
            .thenAccept(res -> Platform.runLater(this::loadLocali))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    state.set(UIState.ERROR);
                    dialogService.showError("Errore", "Errore nella creazione del locale: " + ex.getMessage());
                });
                return null;
            });
    }

    public void updateLocale(Locale locale) {
        if (locale == null || locale.getLinks().isEmpty()) return;
        String updateUrl = locale.getLinkHref("self");

        state.set(UIState.LOADING);
        service.updateLocale(updateUrl, locale)
            .thenAccept(res -> Platform.runLater(this::loadLocali))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    state.set(UIState.ERROR);
                    dialogService.showError("Errore", "Errore nella modifica del locale: " + ex.getMessage());
                });
                return null;
            });
    }

    public void deleteLocale(Locale locale) {
        if (locale == null || locale.getLinks().isEmpty()) return;
        String deleteUrl = locale.getLinkHref("self");

        state.set(UIState.LOADING);
        service.deleteLocale(deleteUrl)
            .thenAccept(v -> Platform.runLater(() -> {
                locali.remove(locale);
                state.set(UIState.SUCCESS);
                dialogService.showInfo("Successo", "Locale eliminato.");
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    state.set(UIState.ERROR);
                    dialogService.showError("Errore", "Errore nell'eliminazione del locale: " + ex.getMessage());
                });
                return null;
            });
    }
}
