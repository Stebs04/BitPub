package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.model.BiliardoStatistiche;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;

/**
 * Controller per la gestione della UI relativa alle statistiche del Biliardo.
 * Coordina il recupero dei dati dal server tramite il client REST unificato
 * e gestisce il rendering asincrono dei componenti grafici in modo sicuro.
 * * @author Luca Franzon
 * @version 1.0
 */
public class BiliardoUIController {

    // --- Costanti ---
    // Centralizziamo le stringhe per facilitare eventuali modifiche future o traduzioni
    private static final String API_ENDPOINT = "/biliardo/statistiche";
    private static final String TESTO_CARICAMENTO = "...";
    private static final String TESTO_ERRORE = "N/D";

    // --- Componenti FXML ---
    @FXML private Label serieMassimaLabel;
    @FXML private ListView<String> storicoListView;
    @FXML private Label statoLabel;
    @FXML private Button btnAggiorna;

    /** Client per le comunicazioni di rete (Modulo unificato di Stefano, Timothy e Luca) */
    private RestClient restClient;

    /**
     * Inizializzatore automatico JavaFX.
     * Predispone il client di rete e avvia un primo caricamento dei dati.
     */
    @FXML
    public void initialize() {
        // Istanziamento del client unico per centralizzare la configurazione HTTP
        this.restClient = new RestClient();
        
        // Avviamo un primo fetch dei dati appena la schermata viene aperta
        caricaDatiBiliardo();
    }

    /**
     * Recupera le statistiche aggiornate dal backend e popola i componenti della dashboard.
     * Utilizza pattern asincroni (CompletableFuture) per garantire la fluidità dell'interfaccia.
     */
    @FXML
    public void caricaDatiBiliardo() {
        // Protezione della UI: disabilitiamo il bottone per evitare doppie richieste.
        // Utilizziamo Platform.runLater perché stiamo manipolando nodi grafici JavaFX.
        Platform.runLater(() -> {
            btnAggiorna.setDisable(true);
            statoLabel.setText("Recupero dati dal server in corso...");
            serieMassimaLabel.setText(TESTO_CARICAMENTO);
        });

        restClient.faiChiamataGet(API_ENDPOINT, BiliardoStatistiche.class)
                .thenAccept(statistiche -> {
                    // REGOLA DI LUCA: Thread Safety
                    // Le modifiche ai nodi grafici devono essere eseguite RIGOROSAMENTE sul thread UI.
                    Platform.runLater(() -> {
                        // Aggiornamento della serie massima registrata
                        serieMassimaLabel.setText(String.valueOf(statistiche.getSerieMassimaPalle()));

                        // Reset e ripopolamento della lista dello storico partite
                        storicoListView.getItems().clear();
                        storicoListView.getItems().addAll(statistiche.getStoricoPartite());
                        
                        statoLabel.setText("Dati aggiornati con successo.");
                        btnAggiorna.setDisable(false);
                    });
                })
                .exceptionally(errore -> {
                    // Gestione degli errori di rete (es. server offline).
                    // Impedisce il crash dell'applicazione e avvisa l'utente del problema.
                    Platform.runLater(() -> {
                        serieMassimaLabel.setText(TESTO_ERRORE);
                        statoLabel.setText("Errore di rete: impossibile recuperare i dati.");
                        btnAggiorna.setDisable(false);
                    });
                    System.err.println("Errore Biliardo: " + errore.getMessage());
                    return null;
                });
    }
}