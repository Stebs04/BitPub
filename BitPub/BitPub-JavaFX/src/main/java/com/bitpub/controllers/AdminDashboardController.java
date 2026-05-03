package com.bitpub.controllers;

import com.bitpub.models.Locale;
import com.bitpub.models.Utente;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaLocali;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Controller per la Dashboard Amministratore.
 * Gestisce l'anagrafica dei locali (CRUD) interfacciandosi con il backend
 * tramite API REST conformi allo standard HATEOAS e Semantic Versioning.
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

    private final ObservableList<Locale> listaLocaliObservable = FXCollections.observableArrayList();

    /**
     * Inizializza la vista configurando il data-binding della tabella e i listener di selezione.
     */
    @FXML
    public void initialize() {
        configuraTabella();
        caricaDati();

        // Listener per la gestione dinamica dell'abilitazione dei controlli basata sulla selezione
        localiTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean rigaSelezionata = newSelection != null;
            btnModifica.setDisable(!rigaSelezionata);
            btnElimina.setDisable(!rigaSelezionata);
        });
    }

    /**
     * Configura il mapping tra le proprietà dell'oggetto Locale e le colonne della TableView.
     */
    private void configuraTabella() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCitta.setCellValueFactory(new PropertyValueFactory<>("citta"));
        colIndirizzo.setCellValueFactory(new PropertyValueFactory<>("indirizzo"));
        localiTable.setItems(listaLocaliObservable);
    }

    /**
     * Esegue il recupero asincrono dei locali dal server Cloud.
     * Inietta l'header di versione v1 per superare il filtro ApiVersionFilter.
     */
    @FXML
    public void caricaDati() {
        progressIndicator.setVisible(true);

        // Uso del RestClient centralizzato per la chiamata GET
        RestClient.getInstance().faiChiamataGet("/api/v1/locali", Locale[].class)
                .thenAccept(localiArray -> {
                    Platform.runLater(() -> {
                        if (localiArray != null) {
                            listaLocaliObservable.setAll(Arrays.asList(localiArray));
                            System.out.println("Lista aggiornata con " + localiArray.length + " locali.");
                        } else {
                            listaLocaliObservable.clear();
                            System.out.println("Nessun locale presente o array nullo.");
                        }
                        progressIndicator.setVisible(false);
                    });
                })
                .exceptionally(e -> {
                    System.err.println("Dettaglio errore di rete in caricaDati:");
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        mostraNotifica("Errore Connessione", "Impossibile contattare il server.", Alert.AlertType.ERROR);
                    });
                    return null;
                });
    }

    /**
     * Gestisce l'apertura della procedura per la creazione di un nuovo locale.
     * Metodo collegato all'onAction del file FXML.
     */
    @FXML
    public void handleNuovoLocale() {
        progressIndicator.setVisible(true);

        // 1. Scarichiamo la lista degli utenti (idealmente filtrando per GESTORE se l'API lo supporta)
        // Se non supporta filtri, scarichiamo tutti e filtriamo in Java. Supponiamo che l'API supporti ?role=GESTORE
        RestClient.getInstance().faiChiamataGet("/api/v1/users?role=GESTORE", Utente[].class)
                .thenAccept(utentiArray -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        List<Utente> gestoriDisponibili = utentiArray != null ? Arrays.asList(utentiArray) : List.of();
                        mostraDialogCreazione(gestoriDisponibili);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        mostraNotifica("Errore di rete", "Impossibile recuperare la lista dei gestori.", Alert.AlertType.ERROR);
                        // Procediamo comunque con una lista vuota in caso di errore
                        mostraDialogCreazione(List.of());
                    });
                    return null;
                });
    }

    /**
     * Metodo privato di supporto per costruire e mostrare il Dialog di creazione.
     * Separato da handleNuovoLocale per gestire la natura asincrona della chiamata API.
     */
    private void mostraDialogCreazione(List<Utente> gestoriDisponibili) {
        // Creazione di un Dialog personalizzato
        Dialog<Locale> dialog = new Dialog<>();
        dialog.setTitle("Nuovo Locale");
        dialog.setHeaderText("Inserisci i dati del nuovo locale");

        // Imposta i bottoni
        ButtonType creaButtonType = new ButtonType("Crea", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(creaButtonType, ButtonType.CANCEL);

        // Grid con i campi di testo
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nomeInput = new TextField();
        nomeInput.setPromptText("Nome");
        TextField indirizzoInput = new TextField();
        indirizzoInput.setPromptText("Indirizzo");
        TextField cittaInput = new TextField();
        cittaInput.setPromptText("Città");
        TextField ipEdgeInput = new TextField();
        ipEdgeInput.setPromptText("IP Edge");

        // --- NUOVO: ComboBox per la selezione del Gestore ---
        ComboBox<Utente> gestoreCombo = new ComboBox<>();
        gestoreCombo.setItems(FXCollections.observableArrayList(gestoriDisponibili));
        gestoreCombo.setPromptText("Seleziona Gestore...");

        // Definiamo come l'oggetto Utente deve essere visualizzato nella tendina (Mostriamo l'Username)
        gestoreCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Utente utente, boolean empty) {
                super.updateItem(utente, empty);
                if (empty || utente == null) {
                    setText(null);
                } else {
                    setText(utente.getUsername());
                }
            }
        });

        // Questo serve per visualizzare l'elemento selezionato a tendina chiusa
        gestoreCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Utente utente, boolean empty) {
                super.updateItem(utente, empty);
                if (empty || utente == null) {
                    setText(null);
                } else {
                    setText(utente.getUsername());
                }
            }
        });

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeInput, 1, 0);
        grid.add(new Label("Indirizzo:"), 0, 1);
        grid.add(indirizzoInput, 1, 1);
        grid.add(new Label("Città:"), 0, 2);
        grid.add(cittaInput, 1, 2);
        grid.add(new Label("IP Edge:"), 0, 3);
        grid.add(ipEdgeInput, 1, 3);
        grid.add(new Label("Gestore:"), 0, 4); // Aggiunta la Label
        grid.add(gestoreCombo, 1, 4);          // Aggiunta la ComboBox

        dialog.getDialogPane().setContent(grid);

        // Converti il risultato del dialog
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == creaButtonType) {
                Locale loc = new Locale();
                loc.setName(nomeInput.getText());
                loc.setIndirizzo(indirizzoInput.getText());
                loc.setCitta(cittaInput.getText());
                loc.setIpAddressEdge(ipEdgeInput.getText());

                // --- NUOVO: Assegnazione del gestore ID ---
                Utente gestoreSelezionato = gestoreCombo.getValue();
                if (gestoreSelezionato != null) {
                    loc.setGestoreId(gestoreSelezionato.getId());
                }

                return loc;
            }
            return null;
        });

        // Mostra il dialog e aspetta il risultato
        dialog.showAndWait().ifPresent(nuovoLocale -> {
            progressIndicator.setVisible(true);

            // Chiamata POST centralizzata tramite RestClient
            RestClient.getInstance().faiChiamataPost("/api/v1/locali", nuovoLocale, Locale.class)
                    .thenAccept(responseLocale -> {
                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            if (responseLocale != null) {
                                mostraNotifica("Successo", "Locale creato correttamente!", Alert.AlertType.INFORMATION);
                                caricaDati(); // Ricarica la lista aggiornata
                            } else {
                                mostraNotifica("Errore", "Impossibile creare il locale (controlla i log).", Alert.AlertType.ERROR);
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            mostraNotifica("Errore di rete", ex.getMessage(), Alert.AlertType.ERROR);
                        });
                        return null;
                    });
        });
    }

    /**
     * Gestisce la modifica del locale attualmente selezionato in tabella.
     */
    @FXML
    public void handleModifica() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) {
            mostraNotifica("Errore", "Seleziona un locale da aggiornare", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Locale> dialog = new Dialog<>();
        dialog.setTitle("Aggiorna Locale");
        dialog.setHeaderText("Modifica i dati di: " + selezionato.getName());

        ButtonType aggiornaButtonType = new ButtonType("Aggiorna", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(aggiornaButtonType, ButtonType.CANCEL);

        TextField campoNome = new TextField(selezionato.getName());
        TextField campoCitta = new TextField(selezionato.getCitta());

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Nome:"), 0, 0);
        grid.add(campoNome, 1, 0);
        grid.add(new Label("Città:"), 0, 1);
        grid.add(campoCitta, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == aggiornaButtonType) {
                selezionato.setName(campoNome.getText());
                selezionato.setCitta(campoCitta.getText());
                return selezionato;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(localeAggiornato -> {
            progressIndicator.setVisible(true);
            String endpoint = "/api/v1/locali/" + selezionato.getId();

            // Chiamata PUT centralizzata tramite RestClient
            RestClient.getInstance().putAsync(endpoint, localeAggiornato, responseStr -> {
                progressIndicator.setVisible(false);
                mostraNotifica("Successo", "Locale aggiornato!", Alert.AlertType.INFORMATION);
                caricaDati(); // Ricarica la tabella
            });
        });
    }

    /**
     * Gestisce l'eliminazione del locale selezionato inviando una richiesta DELETE al backend.
     * La rimozione dalla lista locale avviene solo dopo la conferma del server,
     * così un successivo "Aggiorna" non riporta il dato eliminato.
     */
    @FXML
    public void handleElimina() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) {
            mostraNotifica("Nessuna selezione", "Seleziona un locale da eliminare.", Alert.AlertType.WARNING);
            return;
        }

        // Dialogo di conferma prima di procedere con l'eliminazione permanente
        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION);
        conferma.setTitle("Elimina Locale");
        conferma.setHeaderText("Eliminazione permanente");
        conferma.setContentText("Sei sicuro di voler eliminare \"" + selezionato.getName() + "\"? L'operazione è irreversibile.");
        conferma.showAndWait().ifPresent(risposta -> {
            if (risposta == ButtonType.OK) {
                progressIndicator.setVisible(true);
                String endpoint = "/api/v1/locali/" + selezionato.getId();

                // Chiamata DELETE asincrona al backend tramite RestClient
                RestClient.getInstance().deleteAsync(endpoint, response -> {
                    // Rimozione dalla lista solo dopo la conferma del server
                    listaLocaliObservable.remove(selezionato);
                    progressIndicator.setVisible(false);
                    mostraNotifica("Successo", "Locale eliminato correttamente.", Alert.AlertType.INFORMATION);
                });
            }
        });
    }

    /**
     * Visualizza un alert informativo o di errore.
     * @param titolo    Titolo della finestra.
     * @param messaggio Messaggio di dettaglio.
     * @param tipo      Tipo di alert (Error, Info, Warning).
     */
    private void mostraNotifica(String titolo, String messaggio, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}