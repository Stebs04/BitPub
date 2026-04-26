package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.model.BiliardoStatistiche;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

/**
 * Controller per la gestione della UI relativa alle statistiche del Biliardo.
 * Coordina il recupero dei dati dal server tramite il client REST unificato
 * e gestisce il rendering asincrono dei componenti grafici.
 * 
 * @author Luca Franzon
 */
public class BiliardoUIController {

    @FXML private Label serieMassimaLabel;
    @FXML private ListView<String> storicoListView;

    /** Client per le comunicazioni di rete (Modulo unificato di Stefano, Timothy e Luca) */
    private RestClient restClient;

    /**
     * Inizializzatore automatico JavaFX.
     * Predispone il client di rete per le chiamate API all'avvio della vista.
     */
    @FXML
    public void initialize() {
        // Stefano: Istanziamento del client unico per centralizzare la configurazione HTTP
        this.restClient = new RestClient();
    }

    /**
     * Recupera le statistiche aggiornate dal backend e popola i componenti della dashboard.
     * Utilizza pattern asincroni per garantire la fluidità dell'interfaccia.
     */
    @FXML
    public void caricaDatiDashboard() {
        /**
         * LOGICA DI TIMOTHY & LUCA:
         * Esecuzione chiamata GET con iniezione automatica degli header di versione.
         * La deserializzazione in BiliardoStatistiche è delegata al motore GSON interno.
         */
        restClient.faiChiamataGet("/biliardo/statistiche", BiliardoStatistiche.class)
                .thenAccept(statistiche -> {

                    // REGOLA DI LUCA: Thread Safety
                    // Le modifiche ai nodi grafici (Label, ListView) devono essere eseguite sul thread UI
                    Platform.runLater(() -> {
                        // Aggiornamento della serie massima registrata
                        serieMassimaLabel.setText(String.valueOf(statistiche.getSerieMassimaPalle()));

                        // Reset e ripopolamento della lista dello storico partite
                        storicoListView.getItems().clear();
                        storicoListView.getItems().addAll(statistiche.getStoricoPartite());
                    });

                })
                .exceptionally(errore -> {
                    /**
                     * Gestione degli errori di rete (es. server offline o timeout).
                     * Impedisce il crash dell'applicazione loggando l'eccezione.
                     */
                    System.err.println("Errore durante il recupero dei dati biliardo: " + errore.getMessage());
                    return null;
                });
    }
}
