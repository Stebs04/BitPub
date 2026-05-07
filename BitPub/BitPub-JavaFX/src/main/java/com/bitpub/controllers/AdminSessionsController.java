package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
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
 * Controller per la Dashboard Amministratore delle Sessioni.
 * Gestisce l'interfaccia di monitoraggio in tempo reale e il controllo operativo sulle sessioni di gioco attive.
 * Implementa le direttive del paradigma HATEOAS operando come client passivo: le azioni disponibili 
 * e i percorsi di rete vengono derivati dinamicamente dalle risposte del server.
 * Il ciclo di aggiornamento si appoggia all'infrastruttura di animazione di JavaFX (Timeline)
 * per garantire thread-safety senza la necessità di manipolare direttamente i thread di sistema.
 *
 * @author Stefano Bellan 20054330
 */
public class AdminSessionsController {

    // Componenti UI della griglia dati per la disamina delle sessioni in corso
    @FXML private TableView<JsonObject> tableActiveSessions;
    @FXML private TableColumn<JsonObject, String> colSessionId;
    @FXML private TableColumn<JsonObject, String> colGameType;
    @FXML private TableColumn<JsonObject, String> colUser;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private TableColumn<JsonObject, String> colScore;
    @FXML private TableColumn<JsonObject, Void> colActions;

    // Componente di feedback visivo per il controllo heartbeat dell'Edge Node
    @FXML private Label lblEdgeStatus;

    // Istanza di comunicazione asincrona verso il backend
    private final RestClient restClient = RestClient.getInstance();
    
    // Riferimento al loop temporale per consentirne la disattivazione controllata
    private Timeline edgeStatusTimeline;

    /**
     * Hook del ciclo di vita invocato da JavaFX a valle della costruzione dell'albero FXML.
     * Si occupa dell'inizializzazione delle policy di binding per la tabella e dell'innesco 
     * del loop reattivo per il controllo di connettività dei nodi periferici.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        loadSessions();

        // Implementazione di un meccanismo di polling non intrusivo ancorato al thread UI.
        // Rispetto ai timer standard, assicura la sincronizzazione delle mutazioni del DOM.
        edgeStatusTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> pollEdgeStatus()));
        edgeStatusTimeline.setCycleCount(Animation.INDEFINITE);
        edgeStatusTimeline.play();
    }

    /**
     * Configura le routine di estrazione dei dati dai payload JSON.
     * Utilizza lambda expression per implementare un parsing difensivo in grado di tollerare
     * l'assenza di chiavi strutturali senza causare crash dell'interfaccia.
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
            
        // Aggregazione customizzata per comporre il referto del punteggio in una singola stringa formattata
        colScore.setCellValueFactory(cellData -> {
            JsonObject session = cellData.getValue();
            String score = (session.has("scoreBlue") ? session.get("scoreBlue").getAsString() : "0") 
                            + " - " + 
                           (session.has("scoreRed") ? session.get("scoreRed").getAsString() : "0");
            return new SimpleStringProperty(score);
        });

        // Configura il generatore di celle personalizzato per la colonna dedicata alle azioni amministrative
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnForceStop = new Button("Forza Chiusura");

            // Blocco di istanziazione statica della cella: formatta il pulsante e aggancia il listener di chiusura
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
     * Interroga l'infrastruttura per valutare l'operatività del gateway edge locale.
     * Segue una pipeline asincrona partendo dalla Root dell'API per isolare l'URL corretto,
     * aggiornando il semaforo visivo in base all'esito.
     */
    private void pollEdgeStatus() {
        // Avvio sequenza di discovery ipermediale
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String edgeStatusUrl = root.getLinks().get("edge-status").getHref();
                return restClient.getAsync(edgeStatusUrl, JsonObject.class);
            })
            .thenAccept(json -> {
                String status = json.has("status") ? json.get("status").getAsString() : "UNKNOWN";
                // Trasferimento coatto sul thread UI per garantire la thread-safety di JavaFX
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
                // Fail-safe visuale in caso di connettività interrotta
                Platform.runLater(() -> {
                    lblEdgeStatus.setText("Edge: ERRORE CONNESSIONE");
                    lblEdgeStatus.setTextFill(Color.web("#dc3545"));
                });
                return null;
            });
    }

    /**
     * Entry-point collegato all'azione manuale dell'amministratore per sincronizzare
     * la griglia delle sessioni col database centrale.
     */
    @FXML
    public void handleRefresh() {
        loadSessions();
    }

    /**
     * Coordina l'estrazione e il rendering della collezione di sessioni attive.
     * Adotta un meccanismo di unboxing flessibile per interpretare le convenzioni
     * del formato HAL o della paginazione generica restituiti dal layer REST.
     */
    private void loadSessions() {
        // Fila la chiamata partendo dall'ancora principale del backend
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String activeSessionsUrl = root.getLinks().get("active-sessions").getHref();
                return restClient.getAsync(activeSessionsUrl, JsonObject.class);
            })
            .thenAccept(rootObject -> {
                JsonArray jsonArray;
                
                // Analisi e scomposizione polimorfica dell'involucro di risposta
                if (rootObject.has("_embedded")) {
                    JsonObject embedded = rootObject.getAsJsonObject("_embedded");
                    String listKey = embedded.keySet().iterator().next(); 
                    jsonArray = embedded.getAsJsonArray(listKey);
                } else if (rootObject.has("content")) {
                    jsonArray = rootObject.getAsJsonArray("content");
                } else if (rootObject.isJsonArray()) {
                    jsonArray = rootObject.getAsJsonArray(); // Fallback nudo
                } else {
                    jsonArray = new JsonArray();
                }

                ObservableList<JsonObject> sessions = FXCollections.observableArrayList();
                for (JsonElement el : jsonArray) {
                    sessions.add(el.getAsJsonObject());
                }

                // Carica il volume di dati processato nella TableView
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
     * Intercetta la richiesta di interruzione anomala di una partita.
     * Tenta prima di seguire un link ipermediale di azione fornito all'interno della risorsa stessa,
     * supportando nativamente i controlli di autorizzazione lato server.
     *
     * @param session Il frammento JSON corrispondente alla riga selezionata in tabella
     */
    private void handleForceStop(JsonObject session) {
        String sessionId = session.has("id") ? session.get("id").getAsString() : "Sconosciuto";
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma Chiusura Forzata");
        confirm.setHeaderText("Stai per forzare la chiusura della sessione ID: " + sessionId);
        confirm.setContentText("Vuoi procedere? L'Edge Node sbloccherà il tavolo.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            
            // Preferenza primaria: utilizzo del trigger azionabile iniettato direttamente nel DTO (HATEOAS puro)
            if (session.has("_links") && session.getAsJsonObject("_links").has("force-stop")) {
                String forceStopUrl = session.getAsJsonObject("_links").getAsJsonObject("force-stop").get("href").getAsString();
                eseguiForceStopAsincrono(forceStopUrl);
            } else {
                // Percorso alternativo: calcolo logico dell'endpoint tramite la direttiva generica fornita dalla Root
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
     * Avvia il trigger remoto per abbattere lo stato della sessione.
     *
     * @param url L'indirizzo puntuale a cui indirizzare l'azione di stato
     */
    private void eseguiForceStopAsincrono(String url) {
        restClient.postAsync(url, new JsonObject(), JsonObject.class)
            .thenAccept(res -> gestisciSuccessoChiusura())
            .exceptionally(this::gestisciErroreChiusura);
    }

    /**
     * Gestisce la notifica di successo della procedura distruttiva
     * e ordina un aggiornamento integrale per riallineare l'interfaccia col database.
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
     * Interceptor degli errori verificatisi durante l'operazione asincrona di chiusura forzata.
     *
     * @param e Eccezione incapsulata prodotta dalla catena di future
     * @return null per soddisfare i requisiti formali della funzione exceptionally
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
     * Regola l'uscita dalla schermata e il ritorno al menu di amministrazione generale.
     * Abbatte sistematicamente l'orologio interno prima di smontare la scena per eliminare referenze pendenti.
     *
     * @param event L'evento emesso in corrispondenza del click dell'utente
     */
    @FXML
    void handleBackToDashboard(ActionEvent event) {
        // Garantisce il cleanup del task schedulato prima della deallocazione della classe
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