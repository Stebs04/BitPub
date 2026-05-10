package com.bitpub.controllers;

import com.bitpub.models.Locale;
import com.bitpub.models.Utente;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Controller per la Dashboard Amministratore dedicata alla gestione dei Locali.
 * 
 * Opera come client ipermediale passivo: non memorizza URL per le operazioni CRUD,
 * ma scopre le capacità del sistema navigando i link HATEOAS forniti dal server.
 * Gestisce la visualizzazione tabellare, la creazione, modifica ed eliminazione delle risorse.
 * 
 * @author Stefano Bellan
 */
public class AdminDashboardController {

    @FXML private TableView<Locale> localiTable;
    @FXML private TableColumn<Locale, Long> colId;
    @FXML private TableColumn<Locale, String> colNome;
    @FXML private TableColumn<Locale, String> colCitta;
    @FXML private TableColumn<Locale, String> colIndirizzo;

    @FXML private Button btnModifica;
    @FXML private Button btnElimina;
    @FXML private ProgressIndicator progressIndicator;

    /** Lista osservabile per il data-binding automatico con la TableView */
    private final ObservableList<Locale> listaLocaliObservable = FXCollections.observableArrayList();
    
    /** Singleton per la gestione delle richieste REST */
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Inizializzazione della vista. Configura il data-binding delle colonne 
     * e imposta i listener per la gestione dinamica della UI.
     */
    @FXML
    public void initialize() {
        configuraTabella();
        caricaDati();

        // Controllo contestuale degli stati: abilita i pulsanti solo se una riga è selezionata
        localiTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean selezionato = (newVal != null);
            btnModifica.setDisable(!selezionato);
            btnElimina.setDisable(!selezionato);
        });
    }

    /**
     * Configura la mappatura tra le proprietà del modello Locale e le colonne della tabella.
     */
    private void configuraTabella() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCitta.setCellValueFactory(new PropertyValueFactory<>("citta"));
        colIndirizzo.setCellValueFactory(new PropertyValueFactory<>("indirizzo"));
        localiTable.setItems(listaLocaliObservable);
    }

    /**
     * Recupera l'elenco dei locali tramite navigazione ipermediale.
     * Segue il workflow: Root -> link "locali" -> GET lista.
     */
    @FXML
    public void caricaDati() {
        Platform.runLater(() -> progressIndicator.setVisible(true));

        // 1. DISCOVERY: Risoluzione dinamica dell'endpoint dalla Root
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String linkLocali = root.getLinkSafe("locali");
                // 2. AZIONE: Recupero asincrono dell'array di oggetti
                return restClient.getAsync(linkLocali, Locale[].class);
            })
            .thenAccept(localiArray -> {
                // 3. UI UPDATE: Aggiornamento della lista osservabile sul thread grafico
                Platform.runLater(() -> {
                    if (localiArray != null) {
                        listaLocaliObservable.setAll(Arrays.asList(localiArray));
                    }
                    progressIndicator.setVisible(false);
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    mostraNotifica("Errore", "Impossibile caricare i locali: " + ex.getMessage(), Alert.AlertType.ERROR);
                });
                return null;
            });
    }

    /**
     * Avvia il workflow per la creazione di un nuovo locale.
     * Recupera preventivamente la lista dei potenziali gestori tramite filtri ipermediali.
     */
    @FXML
    public void handleNuovoLocale() {
        Platform.runLater(() -> progressIndicator.setVisible(true));

        // DISCOVERY: Trova l'endpoint utenti per popolare la selezione dei gestori
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                // Costruzione URL con parametro di filtro per ruolo
                String usersUrl = root.getLinkSafe("users") + "?role=GESTORE";
                return restClient.getAsync(usersUrl, Utente[].class);
            })
            .thenAccept(gestori -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    mostraDialogCreazione(gestori != null ? Arrays.asList(gestori) : List.of());
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    mostraNotifica("Errore", "Impossibile recuperare i gestori.", Alert.AlertType.ERROR);
                });
                return null;
            });
    }

    /**
     * Visualizza la finestra di dialogo per l'inserimento dei dati del nuovo locale.
     * Al termine, esegue il POST all'indirizzo scoperto via HATEOAS.
     * 
     * @param gestori Lista di utenti con ruolo GESTORE disponibili.
     */
    private void mostraDialogCreazione(List<Utente> gestori) {
        // [Logica Dialog omessa per brevità]
        
        // Fase di persistenza: invio del nuovo locale al server
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String createUrl = root.getLinkSafe("locali");
                Locale nuovoLocale = new Locale(); // Dati popolati dal Dialog
                return restClient.postAsync(createUrl, nuovoLocale, Locale.class);
            })
            .thenAccept(res -> Platform.runLater(this::caricaDati));
    }

    /**
     * Gestisce la modifica del locale selezionato utilizzando il link di auto-riferimento (self).
     */
    @FXML
    public void handleModifica() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato == null || selezionato.getLinks().isEmpty()) return;

        // HATEOAS: L'oggetto stesso contiene l'URL per la propria modifica (pattern self-link)
        String updateUrl = selezionato.getLinkHref("self");

        // [Logica Dialog di modifica omessa]

        // Invio aggiornamento via PUT asincrono
        restClient.putAsync(updateUrl, selezionato, Locale.class)
            .thenAccept(res -> Platform.runLater(this::caricaDati));
    }

    /**
     * Gestisce l'eliminazione fisica del locale selezionato dopo conferma dell'utente.
     */
    @FXML
    public void handleElimina() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato == null || selezionato.getLinks().isEmpty()) return;

        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION, "Eliminare " + selezionato.getName() + "?");
        conferma.showAndWait().ifPresent(risposta -> {
            if (risposta == ButtonType.OK) {
                Platform.runLater(() -> progressIndicator.setVisible(true));
                
                // HATEOAS: Navigazione dinamica del link di cancellazione fornito dalla risorsa
                String deleteUrl = selezionato.getLinkHref("self");
                
                restClient.deleteAsync(deleteUrl)
                    .thenAccept(v -> Platform.runLater(() -> {
                        // Aggiornamento ottimistico della UI per reattività immediata
                        listaLocaliObservable.remove(selezionato);
                        progressIndicator.setVisible(false);
                        mostraNotifica("Successo", "Locale eliminato.", Alert.AlertType.INFORMATION);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> progressIndicator.setVisible(false));
                        return null;
                    });
            }
        });
    }

    /**
     * Utility centralizzata per la visualizzazione di messaggi a schermo.
     * 
     * @param titolo Titolo del popup.
     * @param messaggio Testo del messaggio.
     * @param tipo Tipologia di alert (INFO, ERROR, etc.).
     */
    private void mostraNotifica(String titolo, String messaggio, Alert.AlertType tipo) {
        Platform.runLater(() -> {
            Alert alert = new Alert(tipo);
            alert.setTitle(titolo);
            alert.setHeaderText(null);
            alert.setContentText(messaggio);
            alert.showAndWait();
        });
    }
}
