package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.network.RestClient;
import com.bitpub.network.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;

/**
 * Controller di orchestrazione (Shell) per l'interfaccia di amministrazione.
 * Implementa il pattern Single Page Application (SPA) sul framework JavaFX: definisce 
 * uno scheletro fisso per la navigazione e un'area dinamica in cui le diverse sezioni 
 * operative vengono montate e smontate a runtime. Questo garantisce transizioni fluide 
 * senza la necessità di ricaricare l'intera finestra o gestire scene multiple sovrapposte.
 *
 * @author Stefano Bellan 20054330
 */
public class AdminMainController {

    // Area di ancoraggio delegata all'iniezione dei frammenti di interfaccia (moduli FXML)
    @FXML private StackPane contentArea;

    // Riferimento centralizzato all'infrastruttura di rete, predisposto per chiamate asincrone
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Entry point della shell amministrativa.
     * All'avvio dell'involucro principale, forza il montaggio immediato del modulo Dashboard 
     * per evitare all'utente la visualizzazione di un'area contenutistica vuota.
     */
    @FXML
    public void initialize() {
        showDashboard();
    }

    // =========================================================================
    // NAVIGAZIONE MODULI (Iniezione Dinamica)
    // =========================================================================

    // Handler collegati direttamente ai pulsanti del menu di navigazione laterale
    @FXML private void showDashboard() { loadView("/AdminDashboardView.fxml"); }
    @FXML private void showNetworkStatus() { loadView("/AdminNetworkStatus.fxml"); }
    @FXML private void showUsers() { loadView("/AdminUsers.fxml"); }
    @FXML private void showSessions() { loadView("/AdminSessionView.fxml"); }
    @FXML private void showLogs() { loadView("/AdminLogsView.fxml"); }

    /**
     * Centralizza la logica di sostituzione dell'interfaccia (swapping).
     * Pulisce l'albero visivo precedente ed elabora il nuovo file FXML fornito in input.
     * Isola l'intera operazione sul thread dedicato all'interfaccia utente, scongiurando 
     * collisioni o malfunzionamenti grafici legati all'accesso concorrente al DOM.
     *
     * @param fxmlPath Il percorso assoluto o relativo al classpath del file di layout da renderizzare
     */
    private void loadView(String fxmlPath) {
        Platform.runLater(() -> {
            try {
                // Svuota preventivamente l'albero per facilitare il garbage collector ed evitare sovrapposizioni
                contentArea.getChildren().clear();
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent view = loader.load();
                
                // Aggancia la radice del nuovo modulo appena processato all'area visibile
                contentArea.getChildren().add(view);
                
                System.out.println("[AdminMain] Modulo caricato: " + fxmlPath);
            } catch (IOException e) {
                // Registrazione dell'anomalia di caricamento su console standard error per facilitare il debug
                System.err.println("[AdminMain] Errore caricamento vista: " + fxmlPath);
                e.printStackTrace();
            }
        });
    }

    /**
     * Intercetta la richiesta di fine sessione da parte dell'operatore.
     * Provvede alla distruzione forzata del contesto di sicurezza locale, obliterando
     * token e identificativi dell'utente in transito, e redirige il controllo 
     * alla classe principale che si occuperà di ristabilire la schermata di login.
     */
    @FXML
    private void handleLogout() {
        // Obliterazione del contesto di sicurezza autorizzativo locale
        SessionManager.getInstance().logout();
        
        // La notifica al backend cloud potrebbe avvenire seguendo un link HATEOAS di logout;
        // allo stato attuale si forza una de-autenticazione locale e un reload della UI principale
        Platform.runLater(() -> {
            System.out.println("[AdminMain] Logout eseguito, ritorno al Login.");
            Main.eseguiLogout();
        });
    }
}