package com.bitpub.controllers;

import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import com.bitpub.network.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller principale per il layout amministrativo dell'applicazione BitPub.
 * Agisce come contenitore (Shell) per la navigazione dinamica, gestendo lo switch
 * delle sotto-viste e il ciclo di vita della sessione utente.
 *
 * @author Stefano Bellan 20054330
 * @since 2024
 */
public class AdminMainController {

    /** Area di destinazione per l'iniezione dinamica delle viste FXML. */
    @FXML private StackPane contentArea;

    /**
     * Inizializza il controller impostando la Dashboard come schermata predefinita.
     */
    @FXML
    public void initialize() {
        // Caricamento della vista home per l'area amministrativa
        loadView("AdminDashboardView.fxml");
    }

    /** Naviga verso il pannello riepilogativo delle statistiche. */
    @FXML private void showDashboard() { loadView("AdminDashboardView.fxml"); }

    /** Naviga verso il monitoraggio in tempo reale dei nodi Edge. */
   @FXML private void showNetworkStatus() { loadView("AdminNetworkStatus.fxml"); }

    /** Naviga verso la gestione dell'anagrafica e dei ruoli utenti. */
   @FXML private void showUsers()         { loadView("AdminUsers.fxml"); }

    /** Naviga verso il controllo delle sessioni di gioco attive. */
   @FXML private void showSessions()      { loadView("AdminSessionView.fxml"); }

    /** Naviga verso la consultazione dei log di sistema e audit. */
    @FXML private void showLogs() { loadView("AdminLogsView.fxml"); }

    /**
     * Esegue lo switch fisico dei nodi grafici all'interno dello StackPane centrale.
     * Utilizza FXMLLoader per caricare le risorse dal classpath.
     *
     * @param fxmlFile Il nome del file FXML situato nel package delle viste.
     */
    private void loadView(String fxmlFile) {
        try {
            // Risoluzione del percorso risorsa basato sul package com.bitpub.views
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/" + fxmlFile));
            Parent view = loader.load();

            // Sostituzione atomica del contenuto dell'area centrale
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            // Logging tecnico dell'errore di caricamento per la diagnostica
            System.err.println("Errore critico nel caricamento della vista " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gestisce la terminazione della sessione amministrativa.
     * Invalida il token JWT nel Singleton di sessione e ripristina lo Stage alla vista di Login.
     */
    @FXML
    private void handleLogout() {
        System.out.println("[ADMIN] Avvio procedura di logout e pulizia della sessione...");

        // 1. Invalida le credenziali e i dati memorizzati nel SessionManager
        SessionManager.getInstance().logout();

        try {
            // 2. Caricamento della vista di Login tramite FXMLLoader
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoginView.fxml"));
            Parent loginRoot = loader.load();

            // 3. Recupero dello Stage corrente tramite il riferimento ai nodi attivi
            Stage stage = (Stage) contentArea.getScene().getWindow();
            Scene loginScene = new Scene(loginRoot);

            // 4. Ripristino dei metadati dello Stage per la visualizzazione del Login
            stage.setScene(loginScene);
            stage.setTitle("BitPub - Login");
            stage.centerOnScreen();

            System.out.println("[ADMIN] Sessione terminata. Ritorno alla schermata di Login completato.");

        } catch (IOException e) {
            // Gestione dell'eccezione in caso di problemi nel caricamento del file di Login
            System.err.println("Errore fatale durante il reindirizzamento al Login: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
