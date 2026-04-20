package com.bitpub;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Punto di ingresso principale (Entry Point) per l'applicazione JavaFX BitPub.
 * Questa classe si occupa dell'inizializzazione del toolkit grafico, del caricamento
 * dei layout FXML e della gestione della finestra principale (Primary Stage).
 *
 * @author Stefano Bellan 20054330
 */
public class Main extends Application {

    /**
     * Inizializza e visualizza lo stage principale dell'applicazione.
     * Questo metodo viene richiamato automaticamente dal toolkit JavaFX.
     *
     * @param primaryStage Lo stage principale fornito dalla piattaforma.
     * @throws Exception Se il caricamento del file FXML fallisce.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Localizzazione della risorsa FXML per il layout principale
        URL resource = getClass().getResource("/MainLayout.fxml");

        // Gestione preventiva degli errori per risorse mancanti o percorsi errati
        if (resource == null) {
            throw new IllegalArgumentException("Errore Critico: Impossibile trovare il file MainLayout.fxml nel classpath");
        }

        // Parsing del file FXML e costruzione dell'albero dei nodi grafici
        Parent root = FXMLLoader.load(resource);

        // Configurazione delle proprietà della finestra
        primaryStage.setTitle("BitPub - Dashboard Amministratore");

        // Impostazione della scena con risoluzione standard 1024x768 (XGA)
        primaryStage.setScene(new Scene(root, 1024, 768));

        // Visualizzazione effettiva dell'interfaccia utente
        primaryStage.show();
    }

    /**
     * Metodo main standard. Delega l'esecuzione al toolkit JavaFX.
     *
     * @param args Argomenti passati da riga di comando.
     */
    public static void main(String[] args) {
        System.out.println("--- Avvio BitPub JavaFX Dashboard ---");

        // Avvia il ciclo di vita dell'applicazione JavaFX (Metodo bloccante)
        launch(args);
    }
}
