package com.bitpub;

import com.bitpub.network.RestClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Classe entry-point dell'applicazione client BitPub basata sul framework JavaFX.
 * Agisce come orchestratore globale per il ciclo di vita dell'intero applicativo,
 * governando il bootstrap, il routing tra le diverse schermate principali e lo spegnimento
 * sicuro della Java Virtual Machine. Mantiene inoltre il riferimento statico al palcoscenico
 * primario per consentire cambi di scena fluidi senza frammentare il contesto grafico.
 *
 * @author Stefano Bellan 20054330
 */
public class Main extends Application {

    // Riferimento centralizzato alla finestra del sistema operativo per manipolazioni globali
    private static Stage primaryStage;
    
    // Costanti di configurazione applicativa per l'identità visiva e lo stile
    private static final String APP_TITLE = "BitPub - Gaming Ecosystem";
    private static final String GLOBAL_CSS = "/style.css";

    /**
     * Hook principale di avvio invocato automaticamente dal runtime di JavaFX.
     * Struttura la finestra radice, carica le risorse grafiche di base e avvia
     * l'istanza singleton di rete prima di redirigere il controllo alla vista di login.
     *
     * @param stage Il palcoscenico primario fornito dal framework
     */
    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Iniezione difensiva dell'asset grafico per l'icona dell'applicazione
        try {
            primaryStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icon.png"))));
        } catch (Exception e) {
            System.out.println("[Main] Icona non trovata, procedo con quella di default.");
        }

        // Pre-riscaldamento del pool di connessioni HTTP e delle configurazioni Gson in background
        RestClient.getInstance();

        // Trasferimento del controllo al modulo di autenticazione per l'inizio del flusso utente
        navigaVerso("/LoginView.fxml", "BitPub - Login");
    }

    /**
     * Motore di routing globale utilizzato dai vari controller per sostituire interamente
     * la radice dell'interfaccia utente.
     * Incapsula la logica di caricamento FXML e l'applicazione a cascata dei fogli di stile,
     * garantendo l'esecuzione in sicurezza sul JavaFX Application Thread per prevenire
     * collisioni o eccezioni di stato concorrente.
     *
     * @param fxmlPath Il percorso assoluto all'interno del classpath della risorsa descrittiva
     * @param titolo La stringa da proiettare nella barra del titolo della finestra del sistema operativo
     */
    public static void navigaVerso(String fxmlPath, String titolo) {
        Platform.runLater(() -> {
            try {
                // Deserializzazione dell'albero degli elementi grafici
                FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
                Parent root = loader.load();

                // Costruzione del nuovo grafo della scena e aggancio del foglio di stile globale
                Scene scene = new Scene(root);
                String css = Objects.requireNonNull(Main.class.getResource(GLOBAL_CSS)).toExternalForm();
                scene.getStylesheets().add(css);

                // Aggiornamento dello stato della finestra e ricentratura per calibrare i nuovi ingombri
                primaryStage.setTitle(titolo != null ? titolo : APP_TITLE);
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
                primaryStage.show();

                System.out.println("[Navigation] Passaggio a: " + fxmlPath);
            } catch (IOException | NullPointerException e) {
                // Fall-safe per intercettare percorsi errati o risorse non pacchettizzate nel JAR finale
                System.err.println("[Main] Errore critico nel caricamento della vista: " + fxmlPath);
                e.printStackTrace();
            }
        });
    }

    /**
     * Innesca la transizione di ripristino post-autenticazione.
     * Reinizializza il grafo della scena riportando l'applicativo allo stato di ingresso,
     * supportando visivamente le operazioni di clean-up della memoria di sessione.
     */
    public static void eseguiLogout() {
        System.out.println("[Main] Logout richiesto, reset della scena.");
        navigaVerso("/LoginView.fxml", "BitPub - Login");
    }

    /**
     * Reindirizza l'utente alla dashboard corretta in base al suo ruolo
     * dopo un login riuscito.
     */
    public static void redirectDopoLogin() {
        String rawRole = com.bitpub.network.SessionManager.getInstance().getUserRole();
        if (rawRole == null) {
            navigaVerso("/LoginView.fxml", "BitPub - Login");
            return;
        }
        
        // Normalizzazione del ruolo: rimuove il prefisso "ROLE_" se presente e converte in maiuscolo
        String role = rawRole.replace("ROLE_", "").toUpperCase();
        
        switch (role) {
            case "ADMIN":
                navigaVerso("/AdminMainLayout.fxml", "BitPub - Admin Dashboard");
                break;
            case "GESTORE":
                navigaVerso("/GestoreDashboardView.fxml", "BitPub - Gestore Dashboard");
                break;
            case "UTENTE_BASE":
            default:
                navigaVerso("/DashboardView.fxml", "BitPub - Dashboard Utente");
                break;
        }
    }

    /**
     * Hook di teardown invocato dal sistema operativo in fase di chiusura del processo.
     * Agisce come punto di intercettazione per orchestrare lo spegnimento pulito (graceful shutdown)
     * dei task pendenti, delle allocazioni di rete o dei client broker persistenti.
     */
    @Override
    public void stop() {
        System.out.println("[Main] Spegnimento in corso... Pulizia risorse.");
        // Ordina lo spegnimento del toolkit grafico e della relativa istanza della Virtual Machine
        Platform.exit();
        System.exit(0);
    }

    /**
     * Metodo di innesco standard per applicazioni Java.
     * Delega immediatamente l'esecuzione al launcher interno del modulo JavaFX.
     *
     * @param args Parametri iniettati da riga di comando
     */
    public static void main(String[] args) {
        launch(args);
    }
}