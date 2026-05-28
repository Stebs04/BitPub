package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.model.Device;
import com.bitpub.model.Game;
import com.bitpub.network.SessionManager;
import com.bitpub.services.DeviceNetworkService;
import com.bitpub.services.GameNetworkService;
import com.bitpub.utils.JsonManager;
import com.bitpub.model.PageResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.lang.reflect.Type;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class LocalAdminDashboardController {

    private final DeviceNetworkService deviceService;
    private final GameNetworkService gameService;
    private final Gson gson = JsonManager.getGson();

    private UUID currentLocaleId;

    @FXML private TableView<Device> devicesTable;
    @FXML private TableColumn<Device, String> colMac;
    @FXML private TableColumn<Device, String> colStatus;
    @FXML private TableColumn<Device, String> colDate;

    @FXML private TextField macField;
    @FXML private ComboBox<Game> gameComboBox;
    @FXML private Label statusLabel;

    public LocalAdminDashboardController(DeviceNetworkService deviceService, GameNetworkService gameService) {
        this.deviceService = deviceService;
        this.gameService = gameService;
    }

    @FXML
    public void initialize() {
        extractLocaleIdFromJwt();
        setupTable();
        setupGameComboBox();
        loadGames();
        if (currentLocaleId != null) {
            loadDevices();
        } else {
            showError("Attenzione", "Nessun locale associato a questo account Local Admin.");
        }
    }

    private void extractLocaleIdFromJwt() {
        String token = SessionManager.getInstance().getJwtToken();
        if (token == null || token.split("\\.").length < 2) return;
        
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
            if (jsonObject.has("localeIds")) {
                var localeIdsArray = jsonObject.getAsJsonArray("localeIds");
                if (localeIdsArray.size() > 0) {
                    currentLocaleId = UUID.fromString(localeIdsArray.get(0).getAsString());
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nell'estrazione del localeId dal JWT: " + e.getMessage());
        }
    }

    private void setupTable() {
        colMac.setCellValueFactory(new PropertyValueFactory<>("macAddress"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(formatter));
            }
            return new SimpleStringProperty("");
        });
    }

    private void setupGameComboBox() {
        gameComboBox.setConverter(new StringConverter<Game>() {
            @Override
            public String toString(Game game) {
                return game != null ? game.getName() : "";
            }

            @Override
            public Game fromString(String string) {
                return null;
            }
        });
    }

    private void loadGames() {
        gameService.getGames().thenAccept(json -> {
            Type type = new TypeToken<PageResponse<Game>>(){}.getType();
            PageResponse<Game> page = gson.fromJson(json, type);
            Platform.runLater(() -> {
                gameComboBox.setItems(FXCollections.observableArrayList(page.getContent()));
            });
        }).exceptionally(e -> {
            showError("Errore Giochi", "Impossibile recuperare il catalogo dei giochi.");
            return null;
        });
    }

    private void loadDevices() {
        deviceService.getDevicesByLocale(currentLocaleId).thenAccept(json -> {
            Type type = new TypeToken<PageResponse<Device>>(){}.getType();
            PageResponse<Device> page = gson.fromJson(json, type);
            Platform.runLater(() -> {
                devicesTable.setItems(FXCollections.observableArrayList(page.getContent()));
            });
        }).exceptionally(e -> {
            // Se l'endpoint locale/{id} non e' implementato o fallisce, mockiamo i dati temporaneamente per la demo
            System.err.println("Caricamento dispositivi fallito: " + e.getMessage());
            Platform.runLater(() -> {
                statusLabel.setText("Tabella inizializzata (senza rete o dati assenti)");
                statusLabel.setStyle("-fx-text-fill: orange;");
            });
            return null;
        });
    }

    @FXML
    public void registerDevice(ActionEvent event) {
        String mac = macField.getText();
        Game selectedGame = gameComboBox.getValue();

        if (mac == null || mac.trim().isEmpty() || selectedGame == null) {
            statusLabel.setText("Inserisci un MAC Address e seleziona un gioco.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (currentLocaleId == null) {
            statusLabel.setText("Nessun locale associato per questo utente.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        deviceService.registerDevice(mac.trim(), selectedGame.getId(), currentLocaleId)
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Dispositivo registrato con successo!");
                        statusLabel.setStyle("-fx-text-fill: green;");
                        macField.clear();
                        gameComboBox.getSelectionModel().clearSelection();
                        loadDevices(); // Aggiorna tabella
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        statusLabel.setText("Errore durante la registrazione: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: red;");
                    });
                    return null;
                });
    }

    private void showError(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(msg);
            alert.show();
        });
    }

    @FXML
    public void logout(ActionEvent event) {
        Main.eseguiLogout();
    }
}
