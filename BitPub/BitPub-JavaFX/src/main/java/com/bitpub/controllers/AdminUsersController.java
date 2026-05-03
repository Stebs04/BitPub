package com.bitpub.controllers;

import com.bitpub.models.Utente;
import com.bitpub.network.RestClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    @FXML private Button toggleRoleButton;

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
        // URL-encoding della query per gestire spazi e caratteri speciali nel parametro di ricerca
        String queryEncoded = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20");
        String endpoint = "/api/v1/users" + (query.isEmpty() ? "" : "?search=" + queryEncoded);

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

        // URL-encoding dello username per gestire spazi e caratteri speciali nel path
        String usernameEncoded = URLEncoder.encode(selezionato.getUsername(), StandardCharsets.UTF_8).replace("+", "%20");
        String endpoint = "/api/v1/users/" + usernameEncoded + "/toggle-status";

        // Esecuzione della chiamata PUT asincrona
        RestClient.getInstance().putAsync(endpoint, null, response -> {
            // Sincronizzazione dell'interfaccia dopo la conferma del server
            handleSearch();
            System.out.println("Stato utente aggiornato correttamente.");
        });
    }

    /**
     * Alterna il ruolo dell'utente selezionato tra {@code USER} e {@code GESTORE}.
     * Invia una richiesta PUT asincrona all'endpoint {@code /toggle-role} e,
     * a completamento, aggiorna la tabella e mostra un alert di conferma.
     * Gli account con ruolo {@code ADMIN} vengono protetti lato server.
     */
    @FXML
    public void handleToggleRole() {
        // Recupero dell'utente selezionato nella tabella
        Utente selezionato = usersTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) {
            Platform.runLater(() -> {
                Alert avviso = new Alert(Alert.AlertType.WARNING);
                avviso.setTitle("Nessuna selezione");
                avviso.setHeaderText(null);
                avviso.setContentText("Seleziona un utente dalla tabella prima di modificarne il ruolo.");
                avviso.showAndWait();
            });
            return;
        }

        // Protezione lato UI: impedisce di agire sugli account admin
        if ("ADMIN".equalsIgnoreCase(selezionato.getRole())) {
            Platform.runLater(() -> {
                Alert avviso = new Alert(Alert.AlertType.WARNING);
                avviso.setTitle("Operazione non consentita");
                avviso.setHeaderText(null);
                avviso.setContentText("Non è possibile modificare il ruolo di un account ADMIN.");
                avviso.showAndWait();
            });
            return;
        }

        // Calcolo del nuovo ruolo per il testo del dialogo di conferma
        String nuovoRuolo = "GESTORE".equalsIgnoreCase(selezionato.getRole()) ? "USER" : "GESTORE";
        // URL-encoding dello username per gestire spazi e caratteri speciali nel path
        String usernameEncoded = URLEncoder.encode(selezionato.getUsername(), StandardCharsets.UTF_8).replace("+", "%20");
        String endpoint = "/api/v1/users/" + usernameEncoded + "/toggle-role";

        // Richiesta di conferma prima di inviare la modifica
        Platform.runLater(() -> {
            Alert conferma = new Alert(Alert.AlertType.CONFIRMATION);
            conferma.setTitle("Modifica Ruolo");
            conferma.setHeaderText("Cambio ruolo: " + selezionato.getUsername());
            conferma.setContentText(
                "Stai per cambiare il ruolo di \"" + selezionato.getUsername() + "\" in \"" + nuovoRuolo + "\".\nContinuare?"
            );
            conferma.showAndWait().ifPresent(risposta -> {
                if (risposta == ButtonType.OK) {
                    // Esecuzione della chiamata PUT asincrona al backend
                    RestClient.getInstance().putAsync(endpoint, null, response -> {
                        handleSearch();
                        System.out.println("=== Ruolo di " + selezionato.getUsername() + " aggiornato a: " + nuovoRuolo + " ===");
                        Platform.runLater(() -> {
                            Alert ok = new Alert(Alert.AlertType.INFORMATION);
                            ok.setTitle("Ruolo aggiornato");
                            ok.setHeaderText(null);
                            ok.setContentText("Il ruolo di \"" + selezionato.getUsername() + "\" è stato aggiornato a \"" + nuovoRuolo + "\".");
                            ok.show();
                        });
                    });
                }
            });
        });
    }
}
