package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.network.RestClient;
import com.bitpub.network.SessionContext;
import com.bitpub.network.RispostaHateoas;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Controller per la Dashboard Utente principale.
 * * Implementa il pattern "Passive Client" basato sui principi HATEOAS: il controller
 * non conosce staticamente gli endpoint, ma scopre le funzionalità navigando i link 
 * forniti dalla risorsa Root dell'API.
 * * Gestisce l'aggiornamento dinamico del profilo utente e il workflow di attivazione
 * dei servizi (calciobalilla, freccette, biliardo) in modalità asincrona.
 * * @author Stefano Bellan (Refactoring)
 */
public class DashboardController {

    @FXML private Label lblCredit;
    @FXML private Button btnFoosball;
    @FXML private Button btnDarts;
    @FXML private Button btnBilliards;
    @FXML private Button btnLogout;

    /** Client per le chiamate REST, configurato come Singleton */
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Inizializzazione del controller (chiamata automaticamente da JavaFX).
     * Avvia la catena di discovery ipermediale per recuperare le informazioni 
     * del profilo utente e aggiornare il credito a display.
     */
    @FXML 
    public void initialize() {
        // Fase 1: DISCOVERY - Interroga la Root per ottenere il link al profilo personale
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                // Estrazione dinamica dell'URL utente tramite la relazione "me"
                String userUrl = root.getLinkSafe("me");
                return restClient.getAsync(userUrl, JsonObject.class);
            })
            .thenAccept(userData -> {
                // Fase 2: UI UPDATE - Formattazione e visualizzazione del credito
                // Il parsing tiene conto di possibili valori nulli o mancanti dal server
                String credit = userData.has("credit") ? userData.get("credit").getAsString() : "0.00";
                
                // Il thread grafico (UI Thread) deve gestire l'aggiornamento della label
                Platform.runLater(() -> lblCredit.setText("€ " + credit));
            })
            .exceptionally(ex -> {
                // Fallback in caso di timeout o errori di rete
                Platform.runLater(() -> lblCredit.setText("Errore dati"));
                return null;
            });
    }

    /**
     * Gestisce l'evento di clic sul pulsante Calciobalilla.
     * Avvia una nuova sessione di gioco seguendo il workflow HATEOAS.
     * * @param event L'evento ActionEvent generato dal pulsante
     */
    @FXML 
    void handleFoosballClick(ActionEvent event) {
        // Feedback visivo immediato all'utente
        lblCredit.setText("Avvio sessione...");

        // Fase 1: DISCOVERY - Recupera l'endpoint per lo start della sessione
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String startUrl = root.getLinkSafe("foosball-start");
                
                // Fase 2: ACTION - Esegue il POST per la creazione della risorsa sessione
                JsonObject payload = new JsonObject();
                payload.addProperty("table_id", 1); // ID cablato per la versione corrente
                return restClient.postAsync(startUrl, payload, JsonObject.class);
            })
            .thenAccept(session -> {
                // Fase 3: STATE PERSISTENCE - Memorizza i dati ricevuti per uso futuro
                salvaInfoSessione(session);
                
                // Fase 4: NAVIGATION - Switch al controller della partita
                // MODIFICA QUI: Sostituito /FoosballScoreboard.fxml con /CalciobalillaUtenteView.fxml
                Platform.runLater(() -> cambiaScena(event, "/CalciobalillaUtenteView.fxml"));
            })
            .exceptionally(ex -> {
                // Gestione specifica dell'errore di concorrenza (Sessione già esistente)
                if (ex.getMessage().contains("409")) {
                    recuperaSessioneAttiva(event);
                } else {
                    Platform.runLater(() -> mostraAlert("Attenzione", 
                        "Impossibile avviare la sessione: " + ex.getMessage()));
                }
                return null;
            });
    }

    /**
     * Tenta di recuperare una sessione già esistente qualora il server restituisca 
     * un conflitto durante l'avvio. Naviga il link "foosball-current" dalla Root.
     * * @param event L'evento necessario per il cambio scena successivo
     */
    private void recuperaSessioneAttiva(ActionEvent event) {
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String currentUrl = root.getLinkSafe("foosball-current");
                return restClient.getAsync(currentUrl, JsonObject.class);
            })
            .thenAccept(session -> {
                salvaInfoSessione(session);
                // MODIFICA QUI: Sostituito /FoosballScoreboard.fxml con /CalciobalillaUtenteView.fxml
                Platform.runLater(() -> cambiaScena(event, "/CalciobalillaUtenteView.fxml"));
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> mostraAlert("Errore", "Sessione attiva non trovata."));
                return null;
            });
    }

    /**
     * Estrae i metadati della sessione (ID e URL di stato) dal JSON di risposta 
     * e li persiste nel SessionContext dell'applicazione.
     * * @param session L'oggetto JsonObject contenente i dati della sessione e i link
     */
    private void salvaInfoSessione(JsonObject session) {
        long sessionId = session.get("id").getAsLong();
        SessionContext.setCurrentSessionId(sessionId);

        // Parsing dei link HATEOAS interni alla risorsa sessione
        if (session.has("_links")) {
            JsonObject links = session.getAsJsonObject("_links");
            if (links.has("self")) {
                String statusUrl = links.getAsJsonObject("self").get("href").getAsString();
                SessionContext.setCurrentSessionStatusUrl(statusUrl);
            }
        }
    }

    /**
     * Esegue il logout dell'utente corrente e resetta lo stato dell'applicazione.
     * * @param event L'evento ActionEvent generato dal pulsante
     */
    @FXML 
    void handleLogout(ActionEvent event) {
        SessionContext.clearAll();
        // Il metodo delegato si occupa della pulizia dei token e del redirect alla login
        Main.eseguiLogout();
    }

    /** Placeholder per funzionalità non ancora implementate (Freccette) */
    @FXML 
    void handleDartsClick(ActionEvent event) {
        mostraAlert("Info", "Coming soon...");
    }

    /** Placeholder per funzionalità non ancora implementate (Biliardo) */
    @FXML 
    void handleBilliardsClick(ActionEvent event) {
        mostraAlert("Info", "Coming soon...");
    }

    /**
     * Utility per il cambio scena fluido tra diversi file FXML.
     * * @param event L'evento che ha scatenato la navigazione
     * @param fxmlPath Il percorso del file FXML della nuova vista
     */
    private void cambiaScena(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1024, 768));
            stage.show();
        } catch (IOException e) {
            // Log dell'errore critico di caricamento della risorsa grafica
            e.printStackTrace();
        }
    }

    /**
     * Helper per la visualizzazione di messaggi pop-up informativi all'utente.
     * * @param titolo Il titolo della finestra di alert
     * @param messaggio Il corpo del messaggio
     */
    private void mostraAlert(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}