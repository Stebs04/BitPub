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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller della Dashboard per il ruolo ADMIN.
 * Consente la visualizzazione, creazione, modifica e rimozione dei Locali.
 *
 * @author BitPub Team
 * @version 1.0
 */
public class AdminDashboardController {

    @FXML private ProgressIndicator progressIndicator;
    @FXML private TableView<Locale> localiTable;
    @FXML private TableColumn<Locale, Long> colId;
    @FXML private TableColumn<Locale, String> colNome;
    @FXML private TableColumn<Locale, String> colCitta;
    @FXML private TableColumn<Locale, String> colIndirizzo;
    @FXML private TableColumn<Locale, Long> colGestore;

    @FXML private Button btnModifica;
    @FXML private Button btnElimina;

    private static final String API_URL = "http://localhost:8080/api/v1/admin/locali";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    private ObservableList<Locale> localiList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCitta.setCellValueFactory(new PropertyValueFactory<>("citta"));
        colIndirizzo.setCellValueFactory(new PropertyValueFactory<>("indirizzo"));
        colGestore.setCellValueFactory(new PropertyValueFactory<>("gestoreId"));

        localiTable.setItems(localiList);

        localiTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean scelto = (newSel != null);
            btnModifica.setDisable(!scelto);
            btnElimina.setDisable(!scelto);
        });

        caricaDati();
    }

    @FXML
    public void caricaDati() {
        if (!SessionManager.getInstance().isAuthenticated()) {
            mostraErrore("Autenticazione mancante. Effettua il login.");
            return;
        }

        progressIndicator.setVisible(true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + SessionManager.getInstance().getJwtToken())
                .header("Accept", "application/resources.v1+json")
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        if (response.statusCode() == 200) {
                            parsificaLocaliDaHateoas(response.body());
                        } else if (response.statusCode() == 403) {
                            mostraErrore("Accesso negato: Permessi insufficienti.");
                        } else {
                            mostraErrore("Errore del server (" + response.statusCode() + ").");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        mostraErrore("Errore di rete durante il caricamento.");
                    });
                    return null;
                });
    }

    @FXML
    public void handleNuovoLocale(ActionEvent event) {
        mostraFormLocale(null);
    }

    @FXML
    public void handleModifica(ActionEvent event) {
        Locale sel = localiTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            mostraFormLocale(sel);
        }
    }

    @FXML
    public void handleElimina(ActionEvent event) {
        Locale sel = localiTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Sei sicuro di voler eliminare " + sel.getName() + "?");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                eliminaLocaleSelezionato(sel);
            }
        });
    }

    private void mostraFormLocale(Locale existingLocale) {
        // Finestra Modale per Nuovo/Modifica Locale
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existingLocale == null ? "Nuovo Locale" : "Modifica Locale");

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new javafx.geometry.Insets(20));

        TextField tfNome = new TextField(existingLocale != null ? existingLocale.getName() : "");
        tfNome.setPromptText("Nome Locale");
        
        TextField tfCitta = new TextField(existingLocale != null ? existingLocale.getCitta() : "");
        tfCitta.setPromptText("Città");

        TextField tfIndirizzo = new TextField(existingLocale != null ? existingLocale.getIndirizzo() : "");
        tfIndirizzo.setPromptText("Indirizzo");

        TextField tfCapienza = new TextField(existingLocale != null && existingLocale.getCapienza() != null ? String.valueOf(existingLocale.getCapienza()) : "");
        tfCapienza.setPromptText("Capienza");

        TextField tfGestore = new TextField(existingLocale != null && existingLocale.getGestoreId() != null ? String.valueOf(existingLocale.getGestoreId()) : "");
        tfGestore.setPromptText("ID Gestore (opzionale)");

        Button btnSalva = new Button("Salva");
        btnSalva.setOnAction(e -> {
            Locale daSalvare = new Locale();
            if (existingLocale != null) {
                daSalvare.setId(existingLocale.getId());
            }
            daSalvare.setName(tfNome.getText());
            daSalvare.setCitta(tfCitta.getText());
            daSalvare.setIndirizzo(tfIndirizzo.getText());
            
            try {
                if (!tfCapienza.getText().isEmpty()) daSalvare.setCapienza(Integer.parseInt(tfCapienza.getText()));
                if (!tfGestore.getText().isEmpty()) daSalvare.setGestoreId(Long.parseLong(tfGestore.getText()));
            } catch (NumberFormatException ex) {
                mostraErrore("I campi capienza e gestore devono essere numerici");
                return;
            }

            salvaLocale(daSalvare, dialog);
        });

        layout.getChildren().addAll(
                new Label("Nome"), tfNome,
                new Label("Città"), tfCitta,
                new Label("Indirizzo"), tfIndirizzo,
                new Label("Capienza"), tfCapienza,
                new Label("ID Gestore"), tfGestore,
                btnSalva
        );

        dialog.setScene(new Scene(layout, 300, 400));
        dialog.showAndWait();
    }

    private void salvaLocale(Locale loc, Stage dialog) {
        progressIndicator.setVisible(true);

        boolean isUpdate = loc.getId() != null;
        String urlTarget = isUpdate ? API_URL + "/" + loc.getId() : API_URL;

        String body = gson.toJson(loc);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(urlTarget))
                .header("Content-Type", "application/json")
                .header("Accept", "application/resources.v1+json")
                .header("Authorization", "Bearer " + SessionManager.getInstance().getJwtToken());

        if (isUpdate) {
            reqBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
        } else {
            reqBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
        }

        httpClient.sendAsync(reqBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        if (response.statusCode() == 201 || response.statusCode() == 200) {
                            dialog.close();
                            caricaDati(); // Ricarica la lista aggiornata
                        } else {
                            mostraErrore("Operazione fallita (" + response.statusCode() + "): " + response.body());
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        mostraErrore("Impossibile contattare il server.");
                    });
                    return null;
                });
    }

    private void eliminaLocaleSelezionato(Locale sel) {
        progressIndicator.setVisible(true);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "/" + sel.getId()))
                .header("Authorization", "Bearer " + SessionManager.getInstance().getJwtToken())
                .DELETE()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        if (response.statusCode() == 204) {
                            localiList.remove(sel);
                        } else {
                            mostraErrore("Errore durante l'eliminazione (" + response.statusCode() + ")");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        progressIndicator.setVisible(false);
                        mostraErrore("Rete non disponibile");
                    });
                    return null;
                });
    }

    private void parsificaLocaliDaHateoas(String json) {
        try {
            localiList.clear();
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("_embedded")) {
                JsonObject embedded = obj.getAsJsonObject("_embedded");
                // Prendi il primo array dentro _embedded (es. localeList)
                String primoCampo = embedded.keySet().iterator().next();
                JsonArray arr = embedded.getAsJsonArray(primoCampo);
                for (JsonElement el : arr) {
                    Locale l = gson.fromJson(el, Locale.class);
                    localiList.add(l);
                }
            }
        } catch (Exception e) {
            mostraErrore("Parsing JSON fallito");
        }
    }

    private void mostraErrore(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setContentText(msg);
        alert.show();
    }
}