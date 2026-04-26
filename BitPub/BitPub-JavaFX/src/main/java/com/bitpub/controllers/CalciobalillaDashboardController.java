package com.bitpub.controllers;

import com.bitpub.network.AsyncHttpService;
import com.bitpub.network.HttpResponseParser;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;

/**
 * Controller della Dashboard Dati del Calciobalilla.
 * Interagisce con le API REST asincrone per recuperare lo storico e lo stato
 * servendosi dell'architettura HATEOAS, garantendo aggiornamenti Thread Safe per la UI.
 *
 * @author Stefano Bellan 20054330
 */
public class CalciobalillaDashboardController {

    @FXML private ListView<String> listaPartite;
    @FXML private Button btnDettagliSquadra;
    @FXML private Button btnProssimoMatch;
    @FXML private Label statoLabel;

    // Nodi UI aggiunti per garantire la connessione col file FXML aggiornato
    @FXML private Label labelVittorieRossi;
    @FXML private Label labelFalliTotali;
    @FXML private Label labelVittorieBlu;

    private final AsyncHttpService asyncHttpService = new AsyncHttpService();

    /**
     * Avvia il fetch dei dati iniziali al momento del caricamento della vista.
     */
    @FXML
    public void initialize() {
        caricaDatiCalciobalilla();
    }

    /**
     * Invia una richiesta HTTP Asincrona al Cloud per ricevere le statistiche.
     * Tutte le modifiche grafiche successive sono protette da Platform.runLater().
     */
    @FXML
    public void caricaDatiCalciobalilla() {
        statoLabel.setText("Download dati in corso...");
        btnDettagliSquadra.setDisable(true);
        btnProssimoMatch.setDisable(true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/calciobalilla"))
                .header("Accept", "application/resources.v1+json")
                .GET()
                .build();

        asyncHttpService.sendAsync(
                request,
                response -> HttpResponseParser.parseJsonList(response, RispostaHateoas[].class),
                risposteLista -> {
                    // Protezione vitale: Passaggio al Thread JavaFX Principale
                    Platform.runLater(() -> {
                        listaPartite.getItems().clear();

                        if (risposteLista != null && !risposteLista.isEmpty()) {
                            RispostaHateoas primaPartita = risposteLista.get(0);
                            listaPartite.getItems().add("Risorsa HATEOAS caricata con successo dal Server.");

                            // Estrazione HATEOAS dinamica
                            if (primaPartita.getLinks() != null) {
                                if (primaPartita.getLinks().containsKey("dettagli_squadra")) {
                                    String urlDettagli = ((RispostaHateoas.LinkDettaglio)
                                            primaPartita.getLinks().get("dettagli_squadra")).getHref();

                                    btnDettagliSquadra.setDisable(false);
                                    btnDettagliSquadra.setOnAction(evento -> eseguiAzioneDinamica(urlDettagli));
                                }

                                if (primaPartita.getLinks().containsKey("prossimo_match")) {
                                    String urlProssimo = ((RispostaHateoas.LinkDettaglio)
                                            primaPartita.getLinks().get("prossimo_match")).getHref();

                                    btnProssimoMatch.setDisable(false);
                                    btnProssimoMatch.setOnAction(evento -> eseguiAzioneDinamica(urlProssimo));
                                }
                            }
                            statoLabel.setText("Dati caricati e navigazione pronta!");
                            statoLabel.setStyle("-fx-text-fill: #10b981; -fx-font-style: normal; -fx-font-weight: bold;");
                        } else {
                            statoLabel.setText("Nessuna partita presente al momento nel DB.");
                        }
                    });
                },
                errore -> {
                    // Protezione vitale: Gestione sicura dell'errore nella UI
                    Platform.runLater(() -> {
                        statoLabel.setText("Errore di Rete: Impossibile contattare il Server.");
                        statoLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        System.err.println(errore.getMessage());
                    });
                }
        );
    }

    /**
     * Esegue il reindirizzamento o l'azione determinata dal link REST HATEOAS.
     *
     * @param urlHateoas Endpoint estratto dinamicamente fornito dal backend.
     */
    private void eseguiAzioneDinamica(String urlHateoas) {
        System.out.println("Navigazione dinamica in corso: " + urlHateoas);
        statoLabel.setText("Chiamata REST -> " + urlHateoas);
    }
}