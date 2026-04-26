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
 * Controller per la Dashboard Dati del Calciobalilla.
 * Implementa la logica di navigazione ipertestuale definita nella Fase 16,
 * gestendo il parsing dinamico dei link HATEOAS e garantendo la Thread Safety della UI.
 * Gestione multithreading e parse HTTP sicuro apportato tramite AsyncHttpService e HttpResponseParser.
 *
 * @author Stefano Bellan 20054330
 * // Modified by Stefano Bellan 20054330 – Async HTTP layer transition to avoid blocking UI callbacks
 */
public class CalciobalillaDashboardController {

    // --- COMPONENTI UI (Iniezione FXML) ---
    @FXML private ListView<String> listaPartite;
    @FXML private Button btnDettagliSquadra;
    @FXML private Button btnProssimoMatch;
    @FXML private Label statoLabel;

    /** Client per le chiamate REST asincrone */
    private final AsyncHttpService asyncHttpService = new AsyncHttpService();

    /**
     * Inizializzatore automatico JavaFX.
     * Avvia il caricamento dei dati non appena la vista viene caricata in memoria.
     */
    @FXML
    public void initialize() {
        caricaDatiCalciobalilla();
    }

    /**
     * Esegue il recupero asincrono delle partite dal Cloud.
     * Applica le direttive di Luca e Timothy per la gestione sicura del multithreading
     * e l'aggiornamento dinamico dei controlli basato sui link HATEOAS.
     */
    @FXML
    public void caricaDatiCalciobalilla() {
        // Setup iniziale dello stato UI (Feedback visivo di caricamento)
        statoLabel.setText("Download dati in corso...");
        btnDettagliSquadra.setDisable(true);
        btnProssimoMatch.setDisable(true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/calciobalilla"))
                .header("Accept", "application/resources.v1+json")
                .GET()
                .build();

        // Chiamata asincrona via AsyncHttpService
        // - request: richiesta generata sopra
        // - parser: analizza l'array json dal corpo HTTP sul ForkJoinPool
        // - onSuccess: riceve l'oggetto dal servizio sul Thread FX
        // - onError: riceve le Exception sul Thread FX
        asyncHttpService.sendAsync(
                request,
                response -> HttpResponseParser.parseJsonList(response, RispostaHateoas[].class),
                risposteLista -> {
                    // UI update – must run on JavaFX Application Thread
                    listaPartite.getItems().clear();

                    if (risposteLista != null && !risposteLista.isEmpty()) {
                        // Analisi della prima risorsa ipertestuale ricevuta
                        RispostaHateoas primaPartita = risposteLista.get(0);
                        listaPartite.getItems().add("Risorsa HATEOAS caricata correttamente");

                        // LOGICA DI STEFANO (Fase 16): Parsing dinamico del grafo dei link
                        if (primaPartita.getLinks() != null) {

                            // 1. Mapping dinamico per "Dettagli Squadra"
                            if (primaPartita.getLinks().containsKey("dettagli_squadra")) {
                                    String urlDettagli = ((RispostaHateoas.LinkDettaglio)
                                            primaPartita.getLinks().get("dettagli_squadra")).getHref();

                                    btnDettagliSquadra.setDisable(false);
                                    // Binding dinamico dell'azione basato sull'URL fornito dal server
                                    btnDettagliSquadra.setOnAction(evento -> eseguiAzioneDinamica(urlDettagli));
                                }

                                // 2. Mapping dinamico per "Prossimo Match"
                                if (primaPartita.getLinks().containsKey("prossimo_match")) {
                                    String urlProssimo = ((RispostaHateoas.LinkDettaglio)
                                            primaPartita.getLinks().get("prossimo_match")).getHref();

                                    btnProssimoMatch.setDisable(false);
                                    btnProssimoMatch.setOnAction(evento -> eseguiAzioneDinamica(urlProssimo));
                                }
                            }
                            statoLabel.setText("Dati caricati e bottoni connessi via HATEOAS!");
                        } else {
                            statoLabel.setText("Nessuna partita trovata nel database.");
                        }
                },
                errore -> {
                    // UI update – must run on JavaFX Application Thread
                    statoLabel.setText("Errore di Rete: " + errore.getMessage());
                    statoLabel.setStyle("-fx-text-fill: #e74c3c;"); // Colore errore professionale
                }
        );
    }

    /**
     * Orchestratore delle azioni ipertestuali.
     * Centralizza la navigazione verso URL dinamici estratti dai metadati HATEOAS.
     *
     * @param urlHateoas L'endpoint assoluto o relativo fornito dal server.
     */
    private void eseguiAzioneDinamica(String urlHateoas) {
        System.out.println("Navigazione dinamica attivata verso: " + urlHateoas);
        statoLabel.setText("Richiesta REST inviata a: " + urlHateoas);
        // Qui andrebbe la logica per caricare una nuova vista o aggiornare la corrente
    }
}
