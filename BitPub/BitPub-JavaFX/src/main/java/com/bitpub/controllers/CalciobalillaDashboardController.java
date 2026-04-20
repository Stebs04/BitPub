package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

/**
 * Controller per la Dashboard Dati del Calciobalilla.
 * Implementa la logica di navigazione ipertestuale definita nella Fase 16,
 * gestendo il parsing dinamico dei link HATEOAS e garantendo la Thread Safety della UI.
 *
 * @author Stefano Bellan 20054330
 */
public class CalciobalillaDashboardController {

    // --- COMPONENTI UI (Iniezione FXML) ---
    @FXML private ListView<String> listaPartite;
    @FXML private Button btnDettagliSquadra;
    @FXML private Button btnProssimoMatch;
    @FXML private Label statoLabel;

    /** Client unico per le chiamate REST asincrone */
    private final RestClient restClient = new RestClient();

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

        // Chiamata asincrona: utilizziamo un array per ovviare alla Type Erasure dei Generics in GSON
        restClient.faiChiamataGet("/calciobalilla", RispostaHateoas[].class)
                .thenAccept(risposteArray -> {

                    // REGOLE DI LUCA E TIMOTHY: Trasferimento sicuro al JavaFX Application Thread
                    Platform.runLater(() -> {
                        listaPartite.getItems().clear();

                        if (risposteArray != null && risposteArray.length > 0) {
                            // Analisi della prima risorsa ipertestuale ricevuta
                            RispostaHateoas primaPartita = risposteArray[0];
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
                    });
                })
                .exceptionally(errore -> {
                    // Review di Luca: Anche la gestione degli errori deve rispettare la Thread Safety
                    Platform.runLater(() -> {
                        statoLabel.setText("Errore di Rete: " + errore.getMessage());
                        statoLabel.setStyle("-fx-text-fill: #e74c3c;"); // Colore errore professionale
                    });
                    return null;
                });
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
