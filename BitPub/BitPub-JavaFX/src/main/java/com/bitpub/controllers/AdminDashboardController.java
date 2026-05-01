package com.bitpub.controllers;

import com.bitpub.models.Locale;
import com.bitpub.network.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller per la Dashboard Amministratore.
 * Gestisce l'anagrafica dei locali (CRUD) interfacciandosi con il backend 
 * tramite API REST conformi allo standard HATEOAS e Semantic Versioning.
 *
 * @author Stefano Bellan
 * @version 1.0
 * @since 1.0
 */
public class AdminDashboardController {

    /** URL base per le risorse dei locali */
    // FIX: Cambiato l'endpoint da /api/locali a /api/v1/admin/locali
    private static final String API_BASE_URL = "http://localhost:8080/api/v1/admin/locali";
    
    /** * MediaType specifico richiesto dall'ApiVersionFilter del Cloud.
     * L'uso di application/json causerebbe un errore 406 Not Acceptable.
     */
    private static final String MEDIA_TYPE_V1 = "application/resources.v1+json";

    @FXML private TableView<Locale> localiTable;
    @FXML private TableColumn<Locale, Long> colId;
    @FXML private TableColumn<Locale, String> colNome;
    @FXML private TableColumn<Locale, String> colCitta;
    @FXML private TableColumn<Locale, String> colIndirizzo;
    
    @FXML private Button btnModifica;
    @FXML private Button btnElimina;
    @FXML private ProgressIndicator progressIndicator;

    private final ObservableList<Locale> listaLocaliObservable = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL))
                .header("Accept", MEDIA_TYPE_V1)
                .header("Authorization", "Bearer " + SessionManager.getInstance().getJwtToken())
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::processaRispostaServer)
                .exceptionally(e -> {
                    // Sincronizzazione con il JavaFX Application Thread per la manipolazione sicura dei nodi grafici
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        mostraNotifica("Errore Connessione", "Impossibile contattare il server.", Alert.AlertType.ERROR);
                    });
                    return null;
                });
    }

    /**
     * Parsifica la risposta JSON HATEOAS e aggiorna la lista osservabile.
     * @param body Il corpo della risposta JSON ricevuto dal server.
     */
    private void processaRispostaServer(String body) {
        try {
            List<Locale> localiEstratti = com.bitpub.network.HttpResponseParser.parseLocali(body);

            Platform.runLater(() -> {
                // Aggiornamento atomico della lista per riflettere i cambiamenti nella TableView
                listaLocaliObservable.setAll(localiEstratti);
                progressIndicator.setVisible(false);
                System.out.println("Lista aggiornata con " + localiEstratti.size() + " locali.");
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                mostraNotifica("Errore Dati", "Formato risposta non valido.", Alert.AlertType.WARNING);
            });
        }
    }

    /**
     * Gestisce l'apertura della procedura per la creazione di un nuovo locale.
     * Metodo collegato all'onAction del file FXML.
     */
    @FXML
    public void handleNuovoLocale() {
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

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeInput, 1, 0);
        grid.add(new Label("Indirizzo:"), 0, 1);
        grid.add(indirizzoInput, 1, 1);
        grid.add(new Label("Città:"), 0, 2);
        grid.add(cittaInput, 1, 2);
        grid.add(new Label("IP Edge:"), 0, 3);
        grid.add(ipEdgeInput, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Converti il risultato del dialog
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == creaButtonType) {
                Locale loc = new Locale();
                loc.setName(nomeInput.getText());
                loc.setIndirizzo(indirizzoInput.getText());
                loc.setCitta(cittaInput.getText());
                loc.setIpAddressEdge(ipEdgeInput.getText());
                return loc;
            }
            return null;
        });

        // Mostra il dialog e aspetta il risultato
        dialog.showAndWait().ifPresent(nuovoLocale -> {
            com.bitpub.network.AsyncHttpService httpService = new com.bitpub.network.AsyncHttpService();
            String jsonPayload = gson.toJson(nuovoLocale);

            progressIndicator.setVisible(true);

            String tokenUtenteLoggato = SessionManager.getInstance().getJwtToken();
            
            httpService.creaLocaleAsincrono(jsonPayload, tokenUtenteLoggato, "http://localhost:8080")
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        if (response.statusCode() == 201) {
                            mostraNotifica("Successo", "Locale creato correttamente!", Alert.AlertType.INFORMATION);
                            caricaDati(); // Ricarica la lista aggiornata
                        } else {
                            mostraNotifica("Errore (" + response.statusCode() + ")", 
                                "Impossibile creare: " + response.body(), Alert.AlertType.ERROR);
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
        if (selezionato != null) {
            System.out.println("Modifica locale ID: " + selezionato.getId());
            // TODO: Invocazione modale per i dettagli. Nel frattempo diamo un update logico demo
            selezionato.setName(selezionato.getName() + " - Aggiornato");
            
            HttpRequest request = HttpRequest.newBuilder()
                    // FIX: Uso la costante API_BASE_URL per essere sicuri che punti alla rotta dell'Admin
                    .uri(URI.create(API_BASE_URL + "/" + selezionato.getId()))
                    .header("Accept", MEDIA_TYPE_V1)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + SessionManager.getInstance().getJwtToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(selezionato)))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(body -> {
                        System.out.println("Risposta Modifica: " + body);
                        javafx.application.Platform.runLater(this::caricaDati);
                    })
                    .exceptionally(e -> {
                        System.err.println("Errore PUT modifica: " + e.getMessage());
                        return null;
                    });
        }
    }

    @FXML
    public void popupNuovoLocale() {
        handleNuovoLocale();
    }

    /**
     * Gestisce l'aggiornamento (PUT) di un locale.
     */
    @FXML
    public void handleAggiornaLocale() {
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
            String json = String.format("{\"name\":\"%s\", \"citta\":\"%s\"}", 
                    localeAggiornato.getName(), localeAggiornato.getCitta());
            // FIX: Corretto l'endpoint in modo che usi l'API dell'Admin per aggiornare
            String endpoint = "/api/v1/admin/locali/" + selezionato.getId();

            com.bitpub.network.AsyncHttpService httpService = new com.bitpub.network.AsyncHttpService();
            httpService.putAsync(endpoint, json, SessionManager.getInstance().getJwtToken())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        if (response.statusCode() == 200 || response.statusCode() == 204) {
                            mostraNotifica("Successo", "Locale aggiornato!", Alert.AlertType.INFORMATION);
                            caricaDati(); // Ricarica la tabella
                        } else {
                            mostraNotifica("Errore", "Errore " + response.statusCode() + ": " + response.body(), Alert.AlertType.ERROR);
                        }
                    });
                });
        });
    }

    /**
     * Gestisce l'eliminazione logica o fisica del locale selezionato.
     */
    @FXML
    public void handleElimina() {
        Locale selezionato = localiTable.getSelectionModel().getSelectedItem();
        if (selezionato != null) {
            listaLocaliObservable.remove(selezionato);
            mostraNotifica("Successo", "Locale rimosso dalla vista.", Alert.AlertType.INFORMATION);
        }
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