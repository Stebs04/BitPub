package com.bitpub;

import com.bitpub.network.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

/**
 * Entry Point principale per l'ecosistema BitPub.
 * Gestisce il routing dell'applicazione, orchestrando il passaggio tra
 * autenticazione e le diverse aree funzionali basate sui ruoli utente.
 *
 * @author Stefano Bellan
 */
public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setResizable(false); 
        mostraLogin();
    }

    /**
     * Carica e visualizza la schermata di Login/Registrazione.
     */
    public static void mostraLogin() {
        try {
            // Nota: Se il login è in un file separato, cambia "/RegistrazioneView.fxml"
            cambiaScena("/RegistrazioneView.fxml", "BitPub - Benvenuto", 800, 600);
        } catch (IOException e) {
            System.err.println("Errore critico nel caricamento del Login: " + e.getMessage());
        }
    }

    /**
     * NUOVO METODO: Permette a qualsiasi controller di cambiare la schermata facilmente.
     */
    public static void navigaVerso(String fxml, String titolo) {
        try {
            cambiaScena(fxml, titolo, 800, 600);
        } catch (IOException e) {
            System.err.println("Errore di navigazione verso " + fxml + ": " + e.getMessage());
        }
    }

    /**
     * Analizza il ruolo dell'utente nel SessionManager e carica la dashboard corretta.
     * Questo metodo deve essere chiamato dai controller dopo un login di successo.
     */
    public static void redirectDopoLogin() {
        String ruolo = SessionManager.getInstance().getCurrentRole();
        
        // Abilitiamo il ridimensionamento per le dashboard amministrative
        primaryStage.setResizable(true);

        try {
            if ("ADMIN".equalsIgnoreCase(ruolo)) {
                // L'admin vede il layout con la sidebar di navigazione
                cambiaScena("/MainLayout.fxml", "BitPub - Dashboard Amministratore", 1024, 768);
            } else if ("GESTORE".equalsIgnoreCase(ruolo)) {
                // Il gestore viene indirizzato alla sua vista specifica
                cambiaScena("/GestoreDashboardView.fxml", "BitPub - Portale Gestore", 1024, 768);
            } else {
                // Utente standard o fallback
                cambiaScena("/DashboardView.fxml", "BitPub - Dashboard Utente", 1024, 768);
            }
        } catch (IOException e) {
            System.err.println("Errore nel redirect post-login: " + e.getMessage());
        }
    }

    /**
     * Metodo helper privato per la gestione del cambio scena (Root Swapping).
     * Ottimizza il caricamento delle risorse e centra la finestra.
     *
     * @param fxml Percorso della risorsa FXML.
     * @param titolo Titolo della finestra.
     * @param width Larghezza.
     * @param height Altezza.
     * @throws IOException Se il file FXML non è raggiungibile.
     */
    private static void cambiaScena(String fxml, String titolo, double width, double height) throws IOException {
        URL resource = Main.class.getResource(fxml);
        if (resource == null) {
            throw new IOException("Risorsa non trovata: " + fxml);
        }

        Parent root = FXMLLoader.load(resource);
        Scene scene = new Scene(root, width, height);

        primaryStage.setTitle(titolo);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}