package com.bitpub.viewmodels;

import com.bitpub.core.DialogService;
import com.bitpub.core.UIState;
import com.bitpub.javafx.model.LeaderboardEntryModel;
import com.bitpub.services.StatsService;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AdvancedStatsViewModel {
    private final StatsService statsService;
    private final DialogService dialogService;

    private final ObservableList<LeaderboardEntryModel> tableData = FXCollections.observableArrayList();
    private final UIState<Void> actionState = new UIState<>();
    
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final IntegerProperty totalPages = new SimpleIntegerProperty(1);
    private final StringProperty pageInfo = new SimpleStringProperty("Pagina 1 di 1");

    private int currentViewIndex = 0; // 0 for Global, 1 for Game

    public AdvancedStatsViewModel(StatsService statsService, DialogService dialogService) {
        this.statsService = statsService;
        this.dialogService = dialogService;

        currentPage.addListener((obs, oldVal, newVal) -> updatePageInfo());
        totalPages.addListener((obs, oldVal, newVal) -> updatePageInfo());
    }

    public ObservableList<LeaderboardEntryModel> getTableData() { return tableData; }
    public UIState<Void> getActionState() { return actionState; }
    public IntegerProperty currentPageProperty() { return currentPage; }
    public IntegerProperty totalPagesProperty() { return totalPages; }
    public StringProperty pageInfoProperty() { return pageInfo; }

    public void setViewIndex(int index) {
        this.currentViewIndex = index;
    }

    public void refresh() {
        currentPage.set(0);
        loadData();
    }

    public void nextPage() {
        if (currentPage.get() < totalPages.get() - 1) {
            currentPage.set(currentPage.get() + 1);
            loadData();
        }
    }

    public void prevPage() {
        if (currentPage.get() > 0) {
            currentPage.set(currentPage.get() - 1);
            loadData();
        }
    }

    private void loadData() {
        actionState.setLoading();
        
        java.util.concurrent.CompletableFuture<java.util.List<LeaderboardEntryModel>> future;
        if (currentViewIndex == 0) {
            future = statsService.getGlobalLeaderboard(currentPage.get());
        } else {
            future = statsService.getGameLeaderboard("some-game-id", currentPage.get());
        }

        future.thenAccept(list -> {
            Platform.runLater(() -> {
                tableData.setAll(list);
                actionState.setSuccess(null);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> dialogService.showError("Errore nel caricamento statistiche", ex.getMessage()));
            actionState.setError(ex.getMessage());
            return null;
        });
    }

    private void updatePageInfo() {
        pageInfo.set("Pagina " + (currentPage.get() + 1) + " di " + Math.max(1, totalPages.get()));
    }
}
