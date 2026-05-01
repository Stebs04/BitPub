package com.bitpub.controllers;

import com.bitpub.models.SystemLog;
import com.bitpub.network.RestClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Controller per la visualizzazione e il filtraggio dei log di sistema nell'area Admin.
 * Gestisce l'interazione tra la TableView di JavaFX e i servizi REST per il monitoraggio.
 *
 * @author Stefano Bellan 20054330
 */
public class AdminLogsController {

    @FXML private TableView<SystemLog> logsTable;
    @FXML private TableColumn<SystemLog, String> colTimestamp;
    @FXML private TableColumn<SystemLog, String> colLevel;
    @FXML private TableColumn<SystemLog, String> colSource;
    @FXML private TableColumn<SystemLog, String> colMessage;
    @FXML private ComboBox<String> filterLevelCombo;

    /** Lista osservabile che funge da data-source per la tabella UI */
    private final ObservableList<SystemLog> masterData = FXCollections.observableArrayList();

    /**
     * Metodo di inizializzazione richiamato automaticamente al caricamento del file FXML.
     * Configura il mapping delle colonne e i valori iniziali dei filtri.
     */
    @FXML
    public void initialize() {
        // Collegamento delle colonne della tabella alle proprietà della classe SystemLog
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        // Popolamento del selettore dei livelli di log per il filtraggio
        filterLevelCombo.setItems(FXCollections.observableArrayList("ALL", "INFO", "WARN", "ERROR"));
        filterLevelCombo.setValue("ALL");

        // Esecuzione del primo caricamento dati
        refreshLogs();
    }

    /**
     * Recupera i log dal Cloud in modo asincrono tramite il client REST di sistema.
     * Applica i filtri selezionati dall'utente nell'interfaccia.
     */
    @FXML
    public void refreshLogs() {
        String level = filterLevelCombo.getValue();
        String endpoint = "/api/v1/system/logs";

        // Costruzione dinamica dell'endpoint in base al filtro di severità
        if (!"ALL".equals(level)) {
            endpoint += "?level=" + level;
        }

        // Utilizzo del RestClient Singleton per la gestione automatica di JWT e headers
        RestClient.getInstance().faiChiamataGet(endpoint, SystemLog[].class)
                .thenAccept(logArray -> {
                    if (logArray != null) {
                        // Aggiornamento della TableView sul thread dedicato alla grafica
                        Platform.runLater(() -> {
                            masterData.setAll(Arrays.asList(logArray));
                            logsTable.setItems(masterData);
                        });
                    }
                })
                .exceptionally(ex -> {
                    // Notifica dell'errore sulla console di sistema in caso di fallimento della rete
                    Platform.runLater(() -> System.err.println("Errore caricamento log: " + ex.getMessage()));
                    return null;
                });
    }
}