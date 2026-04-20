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
 * @author BitPub Team
 */
public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnCalciobalilla;

    @FXML
    private Button btnFreccette;

    @FXML
    private Button btnBiliardo;

    @FXML
    private Label statusLabel;

    /**
     * Inizializzazione automatica al caricamento del FXML.
     * Mostra la dashboard generale come schermata di default.
     */
    @FXML
    public void initialize() {
        mostraDashboard();
    }

    /**
     * Mostra la dashboard generale con i KPI principali.
     */
    @FXML
    public void mostraDashboard() {
        impostaBottoneAttivo(btnDashboard);
        caricaVista("DashboardView.fxml", "Dashboard Generale");
    }

    /**
     * Mostra la vista del modulo Calciobalilla.
     */
    @FXML
    public void mostraCalciobalilla() {
        impostaBottoneAttivo(btnCalciobalilla);
        caricaVista("view/CalciobalillaView.fxml", "Calciobalilla");
    }

    /**
     * Mostra la vista del modulo Freccette.
     */
    @FXML
    public void mostraFreccette() {
        impostaBottoneAttivo(btnFreccette);
        caricaVista("view/FreccetteView.fxml", "Freccette");
    }

    /**
     * Mostra la vista del modulo Biliardo.
     */
    @FXML
    public void mostraBiliardo() {
        impostaBottoneAttivo(btnBiliardo);
        caricaVista("view/BiliardoView.fxml", "Biliardo");
    }

    /**
     * Carica dinamicamente una vista FXML nel pannello centrale con animazione fade.
     *
     * @param nomeFile  Nome del file FXML da caricare dalla cartella resources.
     * @param titolo    Titolo del modulo per logging.
     */
    private void caricaVista(String nomeFile, String titolo) {
        try {
            var url = getClass().getResource("/" + nomeFile);

            // Se la risorsa non esiste, mostra placeholder senza tentare il caricamento
            if (url == null) {
                mostraPlaceholder(titolo);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Node vista = loader.load();

            // Animazione fade-out della vista corrente
            if (!contentArea.getChildren().isEmpty()) {
                Node corrente = contentArea.getChildren().get(0);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(150), corrente);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(vista);
                    animaFadeIn(vista);
                });
                fadeOut.play();
            } else {
                contentArea.getChildren().setAll(vista);
                animaFadeIn(vista);
            }

            if (statusLabel != null) {
                statusLabel.setText("Modulo attivo: " + titolo);
            }

        } catch (IOException e) {
            mostraPlaceholder(titolo);
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
     * Mostra un pannello placeholder per le viste non ancora implementate.
     *
     * @param nomeModulo Nome del modulo da visualizzare nel placeholder.
     */
    private void mostraPlaceholder(String nomeModulo) {
        VBox placeholder = new VBox(12);
        placeholder.setStyle("-fx-alignment: center; -fx-padding: 40;");

        Label icona = new Label("⚙");
        icona.setStyle("-fx-font-size: 48px; -fx-text-fill: #3d85c8; -fx-opacity: 0.6;");

        Label titolo = new Label("Modulo " + nomeModulo);
        titolo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        Label sottotitolo = new Label("Vista in fase di sviluppo");
        sottotitolo.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");

        placeholder.getChildren().addAll(icona, titolo, sottotitolo);

        contentArea.getChildren().setAll(placeholder);
        animaFadeIn(placeholder);
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
