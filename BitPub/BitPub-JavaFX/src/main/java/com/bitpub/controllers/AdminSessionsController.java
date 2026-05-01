package com.bitpub.controllers;

import com.bitpub.models.GameSession;
import com.bitpub.network.RestClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.Arrays;

/**
 * Controller per la gestione e il monitoraggio delle sessioni di gioco attive.
 * Consente agli amministratori di visualizzare lo stato dei tavoli in tempo reale
 * e di intervenire con comandi di sblocco forzato.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
public class AdminSessionsController {

    @FXML private TableView<GameSession> sessionsTable;
    @FXML private TableColumn<GameSession, String> colVenue, colTable, colStart, colStatus;

    /**
     * Inizializza la tabella configurando il data-binding tra le colonne FXML
     * e le proprietà del modello GameSession.
     */
    @FXML
    public void initialize() {
        // Mapping delle colonne della tabella con i campi della classe GameSession
        colVenue.setCellValueFactory(new PropertyValueFactory<>("venueId"));
        colTable.setCellValueFactory(new PropertyValueFactory<>("tableId"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Avvio del caricamento dati asincrono
        loadSessions();
    }

    /**
     * Interroga le API Cloud per ottenere l'elenco delle sessioni attualmente in corso.
     */
    @FXML
    public void loadSessions() {
        // Richiesta GET asincrona tramite RestClient centralizzato
        RestClient.getInstance().faiChiamataGet("/api/v1/admin/sessions/active", GameSession[].class)
                .thenAccept(sessions -> {
                    if (sessions != null) {
                        // Aggiornamento dell'interfaccia sul thread JavaFX
                        Platform.runLater(() ->
                                sessionsTable.setItems(FXCollections.observableArrayList(Arrays.asList(sessions)))
                        );
                    }
                });
    }

    /**
     * Invia un comando di interruzione forzata per la sessione selezionata nella tabella.
     * Utile in caso di malfunzionamenti hardware o sessioni rimaste "appese".
     */
    @FXML
    public void handleForceUnlock() {
        // Recupero dell'elemento attualmente selezionato dall'utente
        GameSession selected = sessionsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Chiamata POST verso l'endpoint di emergenza per forzare lo stop della sessione
        RestClient.getInstance().faiChiamataPost("/api/v1/admin/sessions/stop/" + selected.getSessionId(), null, Void.class)
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        System.out.println("Comando di sblocco inviato con successo!");
                        // Refresh della lista per confermare l'avvenuta chiusura
                        loadSessions();
                    });
                });
    }
}
