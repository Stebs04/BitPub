package com.bitpub.controllers;

import com.bitpub.models.Utente;
import com.bitpub.network.RestClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.Arrays;

/**
 * Controller per la gestione dell'anagrafica utenti all'interno del pannello amministrativo.
 * Permette la ricerca filtrata, la visualizzazione dei saldi e la gestione dello stato degli account.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
public class AdminUsersController {

    @FXML private TextField searchField;
    @FXML private TableView<Utente> usersTable;
    @FXML private TableColumn<Utente, String> colUsername, colEmail, colRole, colStato;
    @FXML private TableColumn<Utente, Double> colCredito;

    /**
     * Inizializza la vista configurando le colonne della TableView e caricando
     * l'elenco completo degli utenti registrati.
     */
    @FXML
    public void initialize() {
        // Mapping delle proprietà del modello Utente con le colonne dell'interfaccia FXML
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colCredito.setCellValueFactory(new PropertyValueFactory<>("credito"));
        colStato.setCellValueFactory(new PropertyValueFactory<>("stato"));

        // Caricamento iniziale dei dati
        handleSearch();
    }

    /**
     * Esegue una ricerca filtrata degli utenti interpellando le API Cloud.
     * Se il campo di ricerca è vuoto, recupera l'intera collezione.
     */
    @FXML
    public void handleSearch() {
        String query = searchField.getText();
        // Costruzione dinamica dell'endpoint con query parameter per il filtraggio
        String endpoint = "/api/v1/users" + (query.isEmpty() ? "" : "?search=" + query);

        RestClient.getInstance().faiChiamataGet(endpoint, Utente[].class)
                .thenAccept(users -> {
                    if (users != null) {
                        // Aggiornamento della lista osservabile sul thread UI di JavaFX
                        Platform.runLater(() -> usersTable.setItems(FXCollections.observableArrayList(Arrays.asList(users))));
                    }
                })
                .exceptionally(ex -> {
                    // Notifica dell'errore di comunicazione in console
                    Platform.runLater(() -> System.err.println("Errore ricerca utenti: " + ex.getMessage()));
                    return null;
                });
    }

    /**
     * Inverte lo stato di attivazione (Attivo/Sospeso) dell'utente selezionato.
     * Invia una richiesta PUT asincrona al server per persistere la modifica.
     */
    @FXML
    public void handleToggleStatus() {
        // Recupero dell'utente selezionato nella tabella
        Utente selezionato = usersTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) return;

        // Definizione dell'endpoint specifico per l'operazione di toggle
        String endpoint = "/api/v1/users/" + selezionato.getUsername() + "/toggle-status";

        // Esecuzione della chiamata PUT asincrona
        RestClient.getInstance().putAsync(endpoint, null, response -> {
            // Sincronizzazione dell'interfaccia dopo la conferma del server
            handleSearch();
            System.out.println("Stato utente aggiornato correttamente.");
        });
    }
}
