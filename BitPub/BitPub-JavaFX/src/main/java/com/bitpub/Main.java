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
 * Entry Point (Punto di ingresso) principale per l'ecosistema BitPub.
 * Orchestra il routing (cioè i "cambi di pagina") dell'applicazione
 * tra autenticazione e aree funzionali.
 *
 * @author Stefano Bellan 20054330
 */
public class Main extends Application {

    /** Il "palcoscenico" principale su cui vengono montate le scene (schermate) */
    private static Stage primaryStage;

    /**
     * Inizializza lo stage principale e avvia l'interfaccia utente.
     *
     * @param stage Lo stage primario fornito dal toolkit JavaFX.
     */
    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        // Impedisce il ridimensionamento della finestra di login per motivi estetici
        primaryStage.setResizable(false);
        mostraLogin();
    }

    /**
     * Carica e visualizza la schermata di Login.
     */
    public static void mostraLogin() {
        try {
            // Ripristina il layout fisso per la vista di autenticazione
            primaryStage.setResizable(false);
            cambiaScena("/LoginView.fxml", "BitPub - Login", 800, 600);
        } catch (IOException e) {
            // Log dell'errore critico in fase di caricamento risorsa
            System.err.println("Errore caricamento Login: " + e.getMessage());
        }
    }

    /**
     * Esegue la procedura di logout globale.
     * Svuota il SessionManager e riporta l'utente alla schermata di login.
     */
    public static void eseguiLogout() {
        // Pulisce il token JWT e il ruolo dalla memoria Singleton
        SessionManager.getInstance().logout();

        // Garantisce che il ritorno al login avvenga sul thread grafico
        Platform.runLater(() -> mostraLogin());
    }

    /**
     * Fornisce un punto di accesso globale per navigare verso una schermata specifica.
     *
     * @param fxml   Il percorso del file FXML relativo alle risorse.
     * @param titolo Il titolo da visualizzare nella barra della finestra.
     */
    public static void navigaVerso(String fxml, String titolo) {
        // Platform.runLater assicura l'esecuzione nel thread JavaFX per prevenire eccezioni
        Platform.runLater(() -> {
            try {
                cambiaScena(fxml, titolo, 800, 600);
            } catch (IOException e) {
                System.err.println("Errore di navigazione verso " + fxml + ": " + e.getMessage());
            }
        });
    }

    /**
     * Gestisce il re-indirizzamento automatico basato sul ruolo utente recuperato dal SessionManager.
     */
    public static void redirectDopoLogin() {
        // Recupero del ruolo utente precedentemente salvato durante l'autenticazione
        String ruolo = SessionManager.getInstance().getUserRole();

        // Controllo di sicurezza: se il ruolo è nullo, forza il ritorno al login
        if (ruolo == null) {
            System.err.println("Nessun ruolo trovato nel SessionManager! Ritorno al login.");
            mostraLogin();
            return;
        }

        // Le dashboard amministrative consentono il ridimensionamento
        primaryStage.setResizable(true);

        Platform.runLater(() -> {
            try {
                // Switching basato su logica condizionale del ruolo
                if ("ADMIN".equalsIgnoreCase(ruolo)) {
                    cambiaScena("/AdminMainLayout.fxml", "BitPub - Dashboard Amministratore", 1024, 768);
                } else if ("GESTORE".equalsIgnoreCase(ruolo)) {
                    cambiaScena("/GestoreDashboardView.fxml", "BitPub - Dashboard Gestore", 1024, 768);
                } else {
                    // Default: Dashboard standard per utenti base
                    cambiaScena("/DashboardUtenteView.fxml", "BitPub - La tua Dashboard", 1024, 768);
                }
            } catch (IOException e) {
                System.err.println("Errore nel caricamento della dashboard per il ruolo " + ruolo + ": " + e.getMessage());
            }
        });
    }

    /**
     * Metodo core per la sostituzione della scena corrente nello Stage principale.
     *
     * @param fxml   Percorso della risorsa FXML.
     * @param titolo Titolo della finestra.
     * @param width  Larghezza desiderata.
     * @param height Altezza desiderata.
     * @throws IOException Se il file FXML non è accessibile o non esiste.
     */
    private static void cambiaScena(String fxml, String titolo, double width, double height) throws IOException {
        URL resource = Main.class.getResource(fxml);
        if (resource == null) {
            throw new IOException("File FXML non trovato: " + fxml);
        }

        // Caricamento del grafico dei nodi dal file FXML
        Parent root = FXMLLoader.load(resource);
        Scene scene = new Scene(root, width, height);

        // Aggiornamento dello Stage primario
        primaryStage.setTitle(titolo);
        primaryStage.setScene(scene);

        // Riposizionamento della finestra al centro dello schermo dell'utente
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /**
     * Punto di avvio dell'applicazione Java.
     * @param args Argomenti della riga di comando.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
