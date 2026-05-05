package com.bitpub.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller principale per la navigazione dell'applicazione BitPub.
 * Gestisce il caricamento dinamico delle schermate all'interno dell'area centrale
 * e l'aggiornamento visivo della barra laterale di navigazione.
 *
 * @author Stefano Bellan (Refactoring)
 */
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnCalciobalilla;
    @FXML private Button btnFreccette;
    @FXML private Button btnBiliardo;
    @FXML private Label statusLabel;

    /**
     * Metodo di inizializzazione chiamato automaticamente da JavaFX.
     * Imposta la schermata di default all'avvio dell'applicazione.
     */
    @FXML
    public void initialize() {
        mostraDashboard();
    }

    /**
     * Carica e mostra la vista generale della Dashboard.
     */
    @FXML
    public void mostraDashboard() {
        impostaBottoneAttivo(btnDashboard);
        caricaVista("DashboardView.fxml", "Dashboard Generale", "📊");
    }

    /**
     * Carica e mostra la vista di gestione del Calciobalilla.
     * Punta al nuovo file FXML unificato per API e simulazione.
     */
    @FXML
    public void mostraCalciobalilla() {
        impostaBottoneAttivo(btnCalciobalilla);
        avviaMainSimulatori();
        caricaVista("CalciobalillaGestione.fxml", "Gestione Calciobalilla", "⚽");
    }

    private void avviaMainSimulatori() {
        new Thread(() -> {
            try {
                // Otteni il path di esecuzione di BitPub-Simulators
                String basePath = System.getProperty("user.dir"); 
                // Visto che siamo in BitPub-JavaFX, risaliamo alla root del parent se necessario
                String simulatorePath = basePath.replace("BitPub-JavaFX", "BitPub-Simulators");
                if (simulatorePath.equals(basePath)) {
                    simulatorePath += "/../BitPub-Simulators"; // fallback 
                }

                System.out.println("[MainController] Avvio mvn exec:java in " + simulatorePath);

                ProcessBuilder pb = new ProcessBuilder("mvn.cmd", "exec:java", "-Dexec.mainClass=com.bitpub.Main");
                pb.directory(new java.io.File(simulatorePath).getCanonicalFile());
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);

                Process p = pb.start();
                System.out.println("[MainController] Processo Simulatore (Calciobalilla) avviato con successo.");
                // p.waitFor(); // Commentato o eseguibile in back
            } catch (Exception e) {
                System.err.println("[MainController] Errore nell'avvio del simulatore Calciobalilla: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Carica e mostra la vista delle Freccette.
     */
    @FXML
    public void mostraFreccette() {
        impostaBottoneAttivo(btnFreccette);
        caricaVista("FreccetteView.fxml", "Freccette", "🎯");
    }

    /**
     * Carica e mostra la vista del Biliardo.
     */
    @FXML
    public void mostraBiliardo() {
        impostaBottoneAttivo(btnBiliardo);
        caricaVista("BiliardoView.fxml", "Biliardo", "🎱");
    }

    /**
     * Carica un file FXML e lo inserisce nell'area di contenuto centrale.
     * Applica una transizione di dissolvenza per rendere il cambio fluido.
     *
     * @param nomeFile Il nome del file FXML da caricare.
     * @param titolo   Il titolo del modulo da mostrare nella status bar.
     * @param icona    L'icona da usare nel caso in cui la vista non sia ancora sviluppata.
     */
    private void caricaVista(String nomeFile, String titolo, String icona) {
        try {
            var url = getClass().getResource("/" + nomeFile);
            if (url == null) {
                mostraPlaceholder(titolo, icona);
                return;
            }
            Node vista = FXMLLoader.load(url);
            sostituisciVistaConAnimazione(vista);
            statusLabel.setText("Modulo: " + titolo);
        } catch (IOException e) {
            System.err.println("Errore nel caricamento della vista: " + nomeFile);
            mostraPlaceholder(titolo, icona);
        }
    }

    /**
     * Esegue una transizione visiva sostituendo il nodo attualmente visualizzato
     * con il nuovo nodo fornito.
     *
     * @param nuovaVista Il nuovo nodo JavaFX da visualizzare.
     */
    private void sostituisciVistaConAnimazione(Node nuovaVista) {
        // Verifica se c'è già una vista caricata per animarne l'uscita
        if (!contentArea.getChildren().isEmpty()) {
            Node corrente = contentArea.getChildren().get(0);
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), corrente);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                contentArea.getChildren().setAll(nuovaVista);
                animaFadeIn(nuovaVista);
            });
            fadeOut.play();
        } else {
            contentArea.getChildren().setAll(nuovaVista);
            animaFadeIn(nuovaVista);
        }
    }

    /**
     * Applica l'effetto di dissolvenza in entrata a un nodo.
     *
     * @param nodo Il nodo a cui applicare l'animazione.
     */
    private void animaFadeIn(Node nodo) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), nodo);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Genera dinamicamente una vista di cortesia quando un modulo FXML non è trovato.
     *
     * @param nomeModulo Il nome del modulo non trovato.
     * @param icona      L'emoji rappresentativa del modulo.
     */
    private void mostraPlaceholder(String nomeModulo, String icona) {
        VBox placeholder = new VBox(15);
        placeholder.setStyle("-fx-alignment: center;");
        Label lblIcona = new Label(icona);
        lblIcona.setStyle("-fx-font-size: 60px; -fx-text-fill: #3b82f6; -fx-opacity: 0.6;");
        Label lblTitolo = new Label(nomeModulo);
        lblTitolo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label lblSottotitolo = new Label("Interfaccia in costruzione...");
        lblSottotitolo.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8;");
        placeholder.getChildren().addAll(lblIcona, lblTitolo, lblSottotitolo);
        sostituisciVistaConAnimazione(placeholder);
        statusLabel.setText("Modulo: " + nomeModulo + " (Lavori in corso)");
    }

    /**
     * Aggiorna lo stile grafico dei bottoni della sidebar per evidenziare la selezione attuale.
     *
     * @param bottoneAttivo Il bottone cliccato dall'utente.
     */
    private void impostaBottoneAttivo(Button bottoneAttivo) {
        String stileNormale = "-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 15px; -fx-alignment: center-left; -fx-padding: 12 15; -fx-cursor: hand; -fx-background-radius: 8;";
        String stileAttivo = "-fx-background-color: #2563eb; -fx-text-fill: #ffffff; -fx-font-size: 15px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-padding: 12 15; -fx-cursor: hand; -fx-background-radius: 8;";

        Button[] bottoni = {btnDashboard, btnCalciobalilla, btnFreccette, btnBiliardo};
        for (Button btn : bottoni) {
            if (btn != null) {
                btn.setStyle(btn == bottoneAttivo ? stileAttivo : stileNormale);
            }
        }
    }
}