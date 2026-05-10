package com.bitpub.controllers;

import com.bitpub.models.EdgeStatus;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

import java.util.Arrays;

/**
 * Controller dedicato al monitoraggio dello stato di salute della rete degli Edge Nodes 
 * distribuiti nei vari locali fisici.
 * L'implementazione segue l'architettura di un client passivo: l'interfaccia non possiede
 * rotte cablate ma naviga l'albero delle risorse HATEOAS per recuperare dinamicamente 
 * le informazioni di stato. La generazione dell'interfaccia utente è flessibile e si modella 
 * sui dati ricevuti, mantenendo un aggiornamento continuo tramite un meccanismo di polling 
 * non bloccante.
 *
 * @author Stefano Bellan 20054330
 */
public class AdminNetworkStatusController {

    // Contenitore fluido responsabile del layout dinamico delle schede dei locali
    @FXML private FlowPane venuesContainer;
    
    // Comando per forzare manualmente la sincronizzazione dello stato di rete
    @FXML private Button refreshButton;

    // Client HTTP per l'orchestrazione delle chiamate asincrone e la navigazione ipermediale
    private final RestClient restClient = RestClient.getInstance();
    
    // Gestore del ciclo temporale per il polling dei dati di monitoraggio
    private Timeline networkPollingTimeline;

    /**
     * Hook di inizializzazione invocato dal framework JavaFX alla costruzione della scena.
     * Instanzia il ciclo temporale delegando l'infrastruttura di animazione di JavaFX
     * per eseguire un aggiornamento automatico ogni 30 secondi, forzando contemporaneamente
     * la prima estrazione dei dati per un feedback immediato.
     */
    @FXML
    public void initialize() {
        // Configurazione del timer reattivo ancorato al thread grafico per aggiornamenti sicuri
        networkPollingTimeline = new Timeline(new KeyFrame(Duration.seconds(30), event -> handleRefresh()));
        networkPollingTimeline.setCycleCount(Animation.INDEFINITE);
        networkPollingTimeline.play();

        // Innesco esplicito della prima transazione HTTP per popolare la dashboard
        handleRefresh();
    }

    /**
     * Coordina il flusso asincrono di recupero dello stato della rete.
     * Applica un vincolo temporaneo sull'interazione dell'operatore, esegue la discovery
     * dell'endpoint di monitoraggio partendo dalla root dell'API, processa la risposta
     * e orchestra la rigenerazione completa delle card informative all'interno del DOM.
     */
    @FXML
    public void handleRefresh() {
        // Inibizione preventiva del controllo di ricarica per prevenire accodamenti di richieste concorrenti
        Platform.runLater(() -> refreshButton.setDisable(true));

        // Avvio della catena di promesse interrogando l'entry point predefinito dell'architettura
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String networkUrl = root.getLinkSafe("network-status");
                
                // Redirezione della richiesta asincrona verso il percorso operativo scoperto
                return restClient.getAsync(networkUrl, EdgeStatus[].class);
            })
            .thenAccept(statusArray -> {
                // Sincronizzazione delle manipolazioni strutturali sul JavaFX Application Thread
                Platform.runLater(() -> {
                    // Reset strutturale del contenitore prima del nuovo inserimento
                    venuesContainer.getChildren().clear();
                    if (statusArray != null) {
                        for (EdgeStatus status : statusArray) {
                            venuesContainer.getChildren().add(createVenueCard(status));
                        }
                    }
                    // Ripristino della disponibilità operativa al completamento del rendering
                    refreshButton.setDisable(false);
                });
            })
            .exceptionally(ex -> {
                // Intercettazione globale delle eccezioni di rete e ripristino sicuro dello stato interattivo
                Platform.runLater(() -> {
                    refreshButton.setDisable(false);
                    System.err.println("[NetworkStatus] Errore: " + ex.getMessage());
                });
                return null;
            });
    }

    /**
     * Genera a runtime un componente grafico isolato per la rappresentazione sintetica
     * dello stato di operatività di un singolo locale periferico.
     * Configura il layout, applica le regole stilistiche in linea e inietta i marcatori cromatici
     * in base alla raggiungibilità del nodo.
     *
     * @param status Il DTO contenente le metriche aggiornate dell'Edge Node
     * @return Una struttura VBox incapsulata pronta per essere agganciata al parent layout
     */
    private VBox createVenueCard(EdgeStatus status) {
        // Inizializzazione e parametrizzazione spaziale della struttura principale
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Composizione testuale per l'identificazione del nodo
        Label nameLabel = new Label(status.getVenueName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Raggruppamento orizzontale per affiancare il marcatore semaforico e l'etichetta testuale
        HBox statusIndicator = new HBox(8);
        statusIndicator.setAlignment(Pos.CENTER);
        Circle dot = new Circle(5);
        Label statusText = new Label(status.getStatus());

        // Traduzione visiva dello stato di connessione
        if ("ONLINE".equalsIgnoreCase(status.getStatus())) {
            dot.setFill(Color.GREEN);
            statusText.setTextFill(Color.GREEN);
        } else {
            dot.setFill(Color.RED);
            statusText.setTextFill(Color.RED);
        }

        statusIndicator.getChildren().addAll(dot, statusText);
        
        // Esposizione del timestamp dell'ultimo battito di cuore (heartbeat) rilevato dal cloud
        Label lastSeen = new Label("Visto: " + status.getLastSeen());
        lastSeen.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");

        card.getChildren().addAll(nameLabel, statusIndicator, lastSeen);
        return card;
    }

    /**
     * Disconnette permanentemente il meccanismo di monitoraggio automatico.
     * Questa chiamata risulta cruciale durante lo smontaggio della vista per evitare 
     * l'esecuzione latente di query HTTP orfane e prevenire perdite di memoria.
     */
    public void stopPolling() {
        if (networkPollingTimeline != null) {
            networkPollingTimeline.stop();
        }
    }
}