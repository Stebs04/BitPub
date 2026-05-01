package com.bitpub.controllers;

import com.bitpub.models.EdgeStatus;
import com.bitpub.network.RestClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Arrays;

/**
 * Controller per la visualizzazione dello stato della rete degli Edge Nodes nel pannello Admin.
 * Gestisce la generazione dinamica di componenti grafiche (Card) all'interno di un FlowPane
 * per monitorare la connettività in tempo reale delle sedi remote.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
public class AdminNetworkStatusController {

    /** Contenitore FXML per la disposizione fluida delle card dei locali. */
    @FXML
    private FlowPane venuesContainer;

    /** Pulsante per l'aggiornamento manuale dei dati di rete. */
    @FXML
    private Button refreshButton;

    /**
     * Inizializzazione automatica della vista.
     * Esegue il primo caricamento dei dati all'apertura della schermata.
     */
    @FXML
    public void initialize() {
        handleRefresh();
    }

    /**
     * Recupera lo stato degli Edge Nodes interrogando le API Cloud tramite RestClient.
     * Implementa una gestione asincrona per non bloccare il thread principale della UI.
     */
    @FXML
    public void handleRefresh() {
        // Disabilitazione temporanea del tasto per prevenire condizioni di race o spam di richieste
        refreshButton.setDisable(true);

        // Chiamata GET centralizzata con gestione dei JWT e della versione API
        RestClient.getInstance().faiChiamataGet("/api/v1/system/network-status", EdgeStatus[].class)
                .thenAccept(statusArray -> {
                    if (statusArray != null) {
                        // Sincronizzazione con il thread grafico di JavaFX per l'aggiornamento visivo
                        Platform.runLater(() -> {
                            visualizzaLocali(statusArray);
                            refreshButton.setDisable(false);
                        });
                    }
                })
                .exceptionally(ex -> {
                    // Gestione degli errori di comunicazione e ripristino dell'interazione UI
                    Platform.runLater(() -> {
                        System.err.println("Errore nel recupero dello stato rete: " + ex.getMessage());
                        refreshButton.setDisable(false);
                    });
                    return null;
                });
    }

    /**
     * Aggiorna graficamente il contenitore inserendo le nuove card generate.
     *
     * @param statuses Array di oggetti {@link EdgeStatus} recuperati dal server.
     */
    private void visualizzaLocali(EdgeStatus[] statuses) {
        // Reset del contenitore per eliminare le card della sessione precedente
        venuesContainer.getChildren().clear();

        for (EdgeStatus status : statuses) {
            // Generazione programmatica e aggiunta della singola card
            VBox card = creaCardLocale(status);
            venuesContainer.getChildren().add(card);
        }
    }

    /**
     * Crea un componente grafico personalizzato (Card) per rappresentare una sede.
     * Implementa stili CSS inline per definire l'estetica e gli indicatori cromatici di stato.
     *
     * @param status I metadati della sede (nome, id, stato, timestamp).
     * @return Un contenitore {@link VBox} formattato come card visiva.
     */
    private VBox creaCardLocale(EdgeStatus status) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setPrefWidth(220);

        // Applicazione di effetti grafici: bordi arrotondati e ombreggiatura (DropShadow)
        card.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Titolo della card: Nome della sede
        Label nameLabel = new Label(status.getVenueName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Identificativo tecnico della sede
        Label idLabel = new Label("ID: " + status.getVenueId());
        idLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        // Layout per l'indicatore di connettività
        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER);

        // Definizione dell'indicatore circolare (Status Dot)
        Circle statusDot = new Circle(6);
        Label statusText = new Label(status.getStatus().toUpperCase());
        statusText.setStyle("-fx-font-weight: bold;");

        // Logica condizionale per la codifica cromatica (Verde = Online, Rosso = Offline)
        if ("ONLINE".equalsIgnoreCase(status.getStatus())) {
            statusDot.setFill(Color.web("#2ecc71"));
            statusText.setTextFill(Color.web("#2ecc71"));
        } else {
            statusDot.setFill(Color.web("#e74c3c"));
            statusText.setTextFill(Color.web("#e74c3c"));
        }

        statusBox.getChildren().addAll(statusDot, statusText);

        // Metadati relativi all'ultima marca temporale di contatto (Last Seen)
        Label lastSeen = new Label("Visto: " + status.getLastSeen());
        lastSeen.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");

        // Assemblaggio finale dei componenti all'interno della card
        card.getChildren().addAll(nameLabel, idLabel, statusBox, lastSeen);

        return card;
    }
}
