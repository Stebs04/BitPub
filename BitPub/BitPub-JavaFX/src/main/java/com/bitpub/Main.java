package com.bitpub;

import com.bitpub.network.SessionManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

/**
 * Entry Point principale per l'ecosistema BitPub.
 * Orchesta il routing dell'applicazione tra autenticazione e aree funzionali.
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
     * Carica la schermata di Login.
     */
    public static void mostraLogin() {
        try {
            // Carica la vista di login definita nelle risorse
            cambiaScena("/LoginView.fxml", "BitPub - Login", 800, 600);
        } catch (IOException e) {
            System.err.println("Errore caricamento Login: " + e.getMessage());
        }
    }

    /**
     * Fornisce un punto di accesso globale per la navigazione imperativa.
     * Permette ai controller (come Login e Registrazione) di richiedere un cambio scena completo.
     *
     * @param fxml   Il percorso assoluto o relativo al classpath del file FXML.
     * @param titolo Il titolo da iniettare nella barra superiore del SO.
     */
    public static void navigaVerso(String fxml, String titolo) {
        Platform.runLater(() -> {
            try {
                cambiaScena(fxml, titolo, 800, 600);
            } catch (IOException e) {
                System.err.println("Errore di navigazione verso " + fxml + ": " + e.getMessage());
            }
        });
    }

    /**
     * Gestisce il re-indirizzamento basato sul ruolo utente dopo l'autenticazione.
     * Risolve l'errore di puntamento a file inesistenti.
     */
    public static void redirectDopoLogin() {
        String ruolo = SessionManager.getInstance().getCurrentRole();
        primaryStage.setResizable(true);

        Platform.runLater(() -> {
            try {
                if ("ADMIN".equalsIgnoreCase(ruolo)) {
                    // L'admin ha il suo layout specifico o quello generale
                    cambiaScena("/AdminDashboardView.fxml", "BitPub - Admin", 1024, 768);
                } else if ("GESTORE".equalsIgnoreCase(ruolo)) {
                    cambiaScena("/GestoreDashboardView.fxml", "BitPub - Gestore", 1024, 768);
                } else {
                    // Carica il layout principale con sidebar per l'utente standard
                    // ASSICURATI che il file si chiami MainLayout.fxml o DashboardView.fxml
                    cambiaScena("/DashboardView.fxml", "BitPub - Dashboard", 1024, 768);
                }
            } catch (IOException e) {
                System.err.println("Errore nel redirect: " + e.getMessage());
            }
        });
    }

    private static void cambiaScena(String fxml, String titolo, double width, double height) throws IOException {
        URL resource = Main.class.getResource(fxml);
        if (resource == null) throw new IOException("File non trovato: " + fxml);

        Parent root = FXMLLoader.load(resource);
        Scene scene = new Scene(root, width, height);
        primaryStage.setTitle(titolo);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) { launch(args); }
}