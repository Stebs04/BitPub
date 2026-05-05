package com.bitpub.controllers;

import com.bitpub.network.RestClient; // DIPENDE DA: RestClient.java (FILE 7)
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdminSessionsController {

    @FXML private TableView<JsonObject> tableActiveSessions;
    @FXML private TableColumn<JsonObject, String> colSessionId;
    @FXML private TableColumn<JsonObject, String> colGameType;
    @FXML private TableColumn<JsonObject, String> colUser;
    @FXML private TableColumn<JsonObject, String> colStatus;
    @FXML private TableColumn<JsonObject, String> colScore;
    @FXML private TableColumn<JsonObject, Void> colActions;

    @FXML private Label lblEdgeStatus;

    private final Gson gson = com.bitpub.utils.JsonManager.getGson();
    private ScheduledExecutorService scheduler;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadSessions();

        // Avvia il polling ogni 5 secondi per verificare lo stato dell'Edge Node
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::pollEdgeStatus, 0, 5, TimeUnit.SECONDS);
    }

    private void setupTableColumns() {
        colSessionId.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("id") ? cellData.getValue().get("id").getAsString() : ""));
            
        colGameType.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("gameType") ? cellData.getValue().get("gameType").getAsString() : ""));
            
        colUser.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("userId") ? cellData.getValue().get("userId").getAsString() : ""));
            
        colStatus.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().has("status") ? cellData.getValue().get("status").getAsString() : ""));
            
        colScore.setCellValueFactory(cellData -> {
            JsonObject session = cellData.getValue();
            String score = (session.has("scoreBlue") ? session.get("scoreBlue").getAsString() : "0") 
                            + " - " + 
                           (session.has("scoreRed") ? session.get("scoreRed").getAsString() : "0");
            return new SimpleStringProperty(score);
        });

        // Configurazione della colonna Azioni con un CellFactory personalizzato per inserire il bottone
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnForceStop = new Button("Forza Chiusura");

            {
                btnForceStop.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btnForceStop.setOnAction(event -> {
                    JsonObject session = getTableView().getItems().get(getIndex());
                    String sessionId = session.get("id").getAsString();
                    handleForceStop(sessionId);
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

    private void pollEdgeStatus() {
        try {
            // Interroga il Cloud Backend per l'ultimo heartbeat noto dall'Edge
            String response = RestClient.getInstance().sendGet("/api/v1/system/edge-status");
            JsonObject json = gson.fromJson(response, JsonObject.class);
            String status = json.has("status") ? json.get("status").getAsString() : "UNKNOWN";

            Platform.runLater(() -> {
                if ("ONLINE".equals(status)) {
                    lblEdgeStatus.setText("Edge: ONLINE");
                    lblEdgeStatus.setTextFill(Color.web("#198754")); // Verde success
                } else {
                    lblEdgeStatus.setText("Edge: OFFLINE");
                    lblEdgeStatus.setTextFill(Color.web("#dc3545")); // Rosso danger
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                lblEdgeStatus.setText("Edge: ERRORE CONNESSIONE");
                lblEdgeStatus.setTextFill(Color.web("#dc3545"));
            });
        }
    }

    @FXML
    public void handleRefresh() {
        loadSessions(); // Ricarica la lista manualmente
    }

    private void loadSessions() {
        new Thread(() -> {
            try {
                // Recupera le sessioni IN_PROGRESS (l'API potrebbe ritornare liste avvolte in HATEOAS, qui assumiamo un JsonArray diretto o adattalo se hai un wrapper _embedded)
                String response = RestClient.getInstance().sendGet("/api/v1/admin/sessions/active");
                
                // Nota: se usi l'EntityModel HATEOAS su una lista, la risposta avrà un campo _embedded
                JsonArray jsonArray;
                JsonObject root = gson.fromJson(response, JsonObject.class);
                if (root.has("_embedded")) {
                    jsonArray = root.getAsJsonObject("_embedded").getAsJsonArray("gameSessionDTOList"); // da adeguare al nome esatto
                } else {
                    jsonArray = gson.fromJson(response, JsonArray.class);
                }

                ObservableList<JsonObject> sessions = FXCollections.observableArrayList();
                if (jsonArray != null) {
                    for (JsonElement el : jsonArray) {
                        sessions.add(el.getAsJsonObject());
                    }
                }

                Platform.runLater(() -> tableActiveSessions.setItems(sessions));
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Errore API");
                    alert.setHeaderText("Impossibile caricare le sessioni attive");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void handleForceStop(String sessionId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma Chiusura Forzata");
        confirm.setHeaderText("Stai per forzare la chiusura della sessione ID: " + sessionId);
        confirm.setContentText("Vuoi procedere? L'Edge Node sbloccherà il tavolo.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    // Invio il comando POST
                    RestClient.getInstance().sendPost("/api/v1/admin/sessions/" + sessionId + "/force-stop", "{}");
                    
                    Platform.runLater(() -> {
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Comando Inviato");
                        success.setHeaderText(null);
                        success.setContentText("La sessione è stata interrotta forzatamente.");
                        success.showAndWait();
                        
                        // Ricarica la tabella dopo la chiusura
                        loadSessions(); 
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        Alert error = new Alert(Alert.AlertType.ERROR);
                        error.setTitle("Errore API");
                        error.setHeaderText("Errore durante l'interruzione");
                        error.setContentText(e.getMessage());
                        error.showAndWait();
                    });
                }
            }).start();
        }
    }

    @FXML
    void handleBackToDashboard(ActionEvent event) {
        // Ferma il polling prima di cambiare scena
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
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
