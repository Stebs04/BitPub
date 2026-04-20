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
 * Controller principale della dashboard BitPub.
 * Gestisce la navigazione tra i moduli (Biliardo, Calciobalilla, Freccette)
 * e il caricamento dinamico delle view nel pannello centrale.
 *
 * View disponibili al momento:
 *   - DashboardView.fxml         → dashboard generale (placeholder)
 *   - CalciobalillaView.fxml     → modulo calciobalilla (implementato)
 *   - BiliardoView.fxml          → modulo biliardo (implementato)
 *   - FreccetteView.fxml         → placeholder (non ancora implementato)
 *
 * @author Stefano Bellan 20054330
 */
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnCalciobalilla;
    @FXML private Button btnFreccette;
    @FXML private Button btnBiliardo;
    @FXML private Label statusLabel;

    /**
     * Inizializzazione automatica al caricamento del FXML.
     * Mostra la dashboard generale come schermata di default.
     */
    @FXML
    public void initialize() {
        mostraDashboard();
    }

    /**
     * Mostra la dashboard generale.
     * Carica DashboardView.fxml se presente, altrimenti mostra placeholder.
     */
    @FXML
    public void mostraDashboard() {
        impostaBottoneAttivo(btnDashboard);
        caricaVista("view/DashboardView.fxml", "Dashboard", "⬡");
    }

    /**
     * Mostra la vista del modulo Calciobalilla.
     * View implementata: CalciobalillaView.fxml
     */
    @FXML
    public void mostraCalciobalilla() {
        impostaBottoneAttivo(btnCalciobalilla);
        caricaVista("view/CalciobalillaView.fxml", "Calciobalilla", "⚽");
    }

    /**
     * Mostra la vista del modulo Freccette.
     * View non ancora implementata: verrà mostrato il placeholder.
     */
    @FXML
    public void mostraFreccette() {
        impostaBottoneAttivo(btnFreccette);
        caricaVista("view/FreccetteView.fxml", "Freccette", "🎯");
    }

    /**
     * Mostra la vista del modulo Biliardo.
     * View implementata: BiliardoView.fxml
     */
    @FXML
    public void mostraBiliardo() {
        impostaBottoneAttivo(btnBiliardo);
        caricaVista("view/BiliardoView.fxml", "Biliardo", "🎱");
    }

    /**
     * Carica dinamicamente una vista FXML nel pannello centrale con animazione fade.
     * Se il file FXML non esiste o non è ancora implementato, mostra un placeholder stilizzato.
     *
     * @param percorso  Percorso del file FXML relativo a resources/
     * @param titolo    Nome del modulo (per statusLabel e placeholder)
     * @param icona     Emoji/icona da mostrare nel placeholder
     */
    private void caricaVista(String percorso, String titolo, String icona) {
        try {
            var url = getClass().getResource("/" + percorso);

            if (url == null) {
                mostraPlaceholder(titolo, icona);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Node vista = loader.load();

            sostituisciVista(vista);

            if (statusLabel != null) {
                statusLabel.setText("Modulo attivo: " + titolo);
            }

        } catch (IOException e) {
            mostraPlaceholder(titolo, icona);
        }
    }

    /**
     * Sostituisce la vista corrente nel contentArea con animazione fade-out/fade-in.
     */
    private void sostituisciVista(Node nuovaVista) {
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
     * Animazione fade-in per la nuova vista caricata.
     */
    private void animaFadeIn(Node nodo) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), nodo);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Mostra un pannello placeholder stilizzato per le viste non ancora implementate.
     *
     * @param nomeModulo Nome del modulo
     * @param icona      Icona/emoji da mostrare
     */
    private void mostraPlaceholder(String nomeModulo, String icona) {
        VBox placeholder = new VBox(16);
        placeholder.setStyle("-fx-alignment: center; -fx-padding: 60;");

        Label lblIcona = new Label(icona);
        lblIcona.setStyle("-fx-font-size: 52px; -fx-text-fill: #3d85c8; -fx-opacity: 0.5;");

        Label lblTitolo = new Label("Modulo " + nomeModulo);
        lblTitolo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0; -fx-font-family: 'Courier New';");

        Label lblSottotitolo = new Label("Vista in fase di sviluppo");
        lblSottotitolo.setStyle("-fx-font-size: 13px; -fx-text-fill: #4a5568; -fx-font-family: 'Courier New';");

        Label lblBadge = new Label("COMING SOON");
        lblBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f6ad55; " +
                "-fx-background-color: #2d1f00; -fx-padding: 4 12; -fx-background-radius: 12; " +
                "-fx-border-color: #f6ad55; -fx-border-width: 1; -fx-border-radius: 12;");

        placeholder.getChildren().addAll(lblIcona, lblTitolo, lblSottotitolo, lblBadge);

        sostituisciVista(placeholder);

        if (statusLabel != null) {
            statusLabel.setText("Modulo attivo: " + nomeModulo + " (placeholder)");
        }
    }

    /**
     * Aggiorna lo stile dei bottoni della sidebar per evidenziare quello attivo.
     *
     * @param bottoneAttivo Il bottone da marcare come selezionato.
     */
    private void impostaBottoneAttivo(Button bottoneAttivo) {
        String stileNormale = "-fx-background-color: transparent; -fx-text-fill: #a0aec0; " +
                "-fx-font-size: 13px; -fx-cursor: hand; -fx-alignment: center-left; " +
                "-fx-padding: 10 16 10 16; -fx-background-radius: 8;";

        String stileAttivo = "-fx-background-color: #1e3a5f; -fx-text-fill: #63b3ed; " +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; " +
                "-fx-alignment: center-left; -fx-padding: 10 16 10 16; -fx-background-radius: 8; " +
                "-fx-border-color: #3d85c8; -fx-border-width: 0 0 0 3; -fx-border-radius: 0;";

        Button[] tuttiBotoni = {btnDashboard, btnCalciobalilla, btnFreccette, btnBiliardo};
        for (Button btn : tuttiBotoni) {
            if (btn != null) {
                btn.setStyle(btn == bottoneAttivo ? stileAttivo : stileNormale);
            }
        }
    }
}
