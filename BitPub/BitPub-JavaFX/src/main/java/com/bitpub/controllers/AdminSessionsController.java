package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Optional;

/**
 * Controller responsabile della gestione della dashboard amministrativa per le sessioni attive.
 * Implementato seguendo i principi di un client ipermediale passivo (HATEOAS), demanda la
 * discovery degli endpoint al server. La reattività dell'interfaccia è garantita dall'uso
 * di Timeline per il polling, evitando il blocco del thread UI e la gestione esplicita dei thread.
 *
 * @author Stefano Bellan (Refactoring)
 */
public class AdminSessionsController {

    @FXML private TableView<JsonObject> tableActiveSessions;
    @FXML private TableColumn<JsonObject, String> colSessionId;
    @FXML private TableColumn<JsonObject, String> colGameType;
    @FXML private TableColumn<JsonObject, String> colUser;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private TableColumn<JsonObject, String> colScore;
    @FXML private TableColumn<JsonObject, Void> colActions;

    @FXML private Label lblEdgeStatus;

    // Istanza singleton del client di rete per le chiamate API
    private final RestClient restClient = RestClient.getInstance();
    
    // Riferimento alla timeline mantenuto a livello di classe per permetterne l'arresto durante la navigazione
    private Timeline edgeStatusTimeline;

    /**
     * Metodo di inizializzazione invocato automaticamente dal framework JavaFX.
     * Configura i binding della tabella, esegue il primo caricamento dei dati
     * e imposta il loop di polling per il monitoraggio dello stato dell'Edge Node.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        loadSessions();

        // REFACTORING: Sostituzione di ScheduledExecutorService con Timeline di JavaFX.
        // Esegue il polling dell'Edge Status ogni 5 secondi, mantenendo sicura l'interazione UI.
        edgeStatusTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> pollEdgeStatus()));
        edgeStatusTimeline.setCycleCount(Animation.INDEFINITE);
        edgeStatusTimeline.play();
    }

    /**
     * Configura le factory per le celle della tabella.
     * Estrae dinamicamente i valori dai JsonObject associati a ogni riga,
     * garantendo tolleranza ai campi mancanti per evitare NullPointerException.
     */
    private void setupTableColumns() {
        colSessionId.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("id") ? cellData.getValue().get("id").getAsString() : ""));
            
        colGameType.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("gameType") ? cellData.getValue().get("gameType").getAsString() : ""));
            
        colUser.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("userId") ? cellData.getValue().get("userId").getAsString() : ""));
            
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("status") ? cellData.getValue().get("status").getAsString() : ""));
            
        // Formattazione custom per comporre lo score combinando i punteggi dei due team
        colScore.setCellValueFactory(cellData -> {
            JsonObject session = cellData.getValue();
            String score = (session.has("scoreBlue") ? session.get("scoreBlue").getAsString() : "0") 
                            + " - " + 
                           (session.has("scoreRed") ? session.get("scoreRed").getAsString() : "0");
            return new SimpleStringProperty(score);
        });

        // Configurazione della colonna Azioni per forzare la chiusura
        // Inietta un bottone interattivo per ogni riga renderizzata
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnForceStop = new Button("Forza Chiusura");

            // Blocco di inizializzazione per lo stile e il binding dell'evento di click
            {
                btnForceStop.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btnForceStop.setOnAction(event -> {
                    JsonObject session = getTableView().getItems().get(getIndex());
                    handleForceStop(session);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnForceStop);
                }
            }
        });
    }

    /**
     * Interroga il backend per verificare lo stato dell'Edge Node.
     * Utilizza un approccio HATEOAS puro: prima recupera la root per ottenere l'URL corretto,
     * poi effettua la chiamata effettiva verso l'endpoint scoperto.
     */
    private void pollEdgeStatus() {
        // DISCOVERY: Trova l'endpoint di stato Edge interrogando la Root
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String edgeStatusUrl = root.getLinks().get("edge-status").getHref();
                return restClient.getAsync(edgeStatusUrl, JsonObject.class);
            })
            .thenAccept(json -> {
                String status = json.has("status") ? json.get("status").getAsString() : "UNKNOWN";
                // UI UPDATE (La Timeline di per sé scatta sul thread UI, ma il thenAccept viaggia su worker, 
                // quindi Platform.runLater rimane obbligatorio)
                Platform.runLater(() -> {
                    if ("ONLINE".equals(status)) {
                        lblEdgeStatus.setText("Edge: ONLINE");
                        lblEdgeStatus.setTextFill(Color.web("#198754"));
                    } else {
                        lblEdgeStatus.setText("Edge: OFFLINE");
                        lblEdgeStatus.setTextFill(Color.web("#dc3545"));
                    }
                });
            })
            .exceptionally(e -> {
                // Gestione elegante degli errori di rete per evitare crash dell'interfaccia
                Platform.runLater(() -> {
                    lblEdgeStatus.setText("Edge: ERRORE CONNESSIONE");
                    lblEdgeStatus.setTextFill(Color.web("#dc3545"));
                });
                return null;
            });
    }

    /**
     * Handler per il bottone di aggiornamento manuale.
     */
    @FXML
    public void handleRefresh() {
        loadSessions();
    }

    /**
     * Recupera l'elenco delle sessioni attive dal backend.
     * Anche in questo caso si parte dalla root per scoprire l'URI corretto.
     * I dati ricevuti vengono normalizzati e caricati nella TableView.
     */
    private void loadSessions() {
        // DISCOVERY: Scopre le sessioni attive partendo dalla root HATEOAS
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String activeSessionsUrl = root.getLinks().get("active-sessions").getHref();
                return restClient.getAsync(activeSessionsUrl, JsonObject.class);
            })
            .thenAccept(rootObject -> {
                JsonArray jsonArray;
                
                // Estrazione dati dinamica dal wrapper HATEOAS
                // Prevede diverse strutture di risposta a seconda di come il backend impacchetta la collection
                if (rootObject.has("_embedded")) {
                    JsonObject embedded = rootObject.getAsJsonObject("_embedded");
                    String listKey = embedded.keySet().iterator().next(); 
                    jsonArray = embedded.getAsJsonArray(listKey);
                } else if (rootObject.has("content")) {
                    jsonArray = rootObject.getAsJsonArray("content");
                } else if (rootObject.isJsonArray()) {
                    jsonArray = rootObject.getAsJsonArray(); // Fallback purista
                } else {
                    jsonArray = new JsonArray();
                }

                ObservableList<JsonObject> sessions = FXCollections.observableArrayList();
                for (JsonElement el : jsonArray) {
                    sessions.add(el.getAsJsonObject());
                }

                // Push dei dati parsati verso il thread grafico
                Platform.runLater(() -> tableActiveSessions.setItems(sessions));
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Errore API");
                    alert.setHeaderText("Impossibile caricare le sessioni attive");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
                return null;
            });
    }

    /**
     * Gestisce la logica di chiusura forzata di una specifica sessione.
     * Richiede conferma all'utente prima di inoltrare la richiesta di stop.
     * Tenta prima di utilizzare i link HATEOAS specifici della sessione e, in caso di fallimento,
     * applica una strategia di fallback calcolando l'URL dalla root.
     *
     * @param session Il JsonObject rappresentante la sessione selezionata nella tabella
     */
    private void handleForceStop(JsonObject session) {
        String sessionId = session.has("id") ? session.get("id").getAsString() : "Sconosciuto";
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma Chiusura Forzata");
        confirm.setHeaderText("Stai per forzare la chiusura della sessione ID: " + sessionId);
        confirm.setContentText("Vuoi procedere? L'Edge Node sbloccherà il tavolo.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            
            // Preferiamo il link ipermediale esposto direttamente sulla risorsa
            if (session.has("_links") && session.getAsJsonObject("_links").has("force-stop")) {
                String forceStopUrl = session.getAsJsonObject("_links").getAsJsonObject("force-stop").get("href").getAsString();
                eseguiForceStopAsincrono(forceStopUrl);
            } else {
                // Fallback: lo scopriamo dalla Root
                // Componiamo l'URI se il backend non fornisce il link di azione diretta sulla sessione
                restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
                    .thenCompose(root -> {
                        String fallbackUrl = root.getLinks().get("sessions").getHref() + "/" + sessionId + "/force-stop";
                        return restClient.postAsync(fallbackUrl, new JsonObject(), JsonObject.class);
                    })
                    .thenAccept(res -> gestisciSuccessoChiusura())
                    .exceptionally(this::gestisciErroreChiusura);
                return;
            }
        }
    }

    /**
     * Esegue materialmente la chiamata POST per arrestare la sessione.
     *
     * @param url L'endpoint finale risolto per l'azione di force-stop
     */
    private void eseguiForceStopAsincrono(String url) {
        restClient.postAsync(url, new JsonObject(), JsonObject.class)
            .thenAccept(res -> gestisciSuccessoChiusura())
            .exceptionally(this::gestisciErroreChiusura);
    }

    /**
     * Callback di successo per l'operazione di chiusura forzata.
     * Avvisa l'utente e ricarica i dati per mantenere la tabella sincronizzata.
     */
    private void gestisciSuccessoChiusura() {
        Platform.runLater(() -> {
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Comando Inviato");
            success.setHeaderText(null);
            success.setContentText("La sessione è stata interrotta forzatamente.");
            success.showAndWait();
            loadSessions();
        });
    }

    /**
     * Callback di errore per l'operazione di chiusura forzata.
     *
     * @param e L'eccezione sollevata durante la chiamata di rete
     * @return null per soddisfare la firma del metodo exceptionally
     */
    private Void gestisciErroreChiusura(Throwable e) {
        Platform.runLater(() -> {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Errore API");
            error.setHeaderText("Errore durante l'interruzione");
            error.setContentText(e.getMessage());
            error.showAndWait();
        });
        return null;
    }

    /**
     * Gestisce il ritorno alla dashboard principale.
     * Ferma il thread di polling per evitare memory leak o esecuzioni fantasma e
     * carica la nuova scena.
     *
     * @param event L'evento di navigazione innescato dalla UI
     */
    @FXML
    void handleBackToDashboard(ActionEvent event) {
        // Interruzione preventiva del polling prima del cambio di contesto
        if (edgeStatusTimeline != null) {
            edgeStatusTimeline.stop();
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AdminDashboardView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}