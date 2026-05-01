package com.bitpub.controllers;

import com.bitpub.models.PartitaFreccette;
import com.bitpub.models.StatisticheFreccette;
import com.bitpub.network.RestClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * Controller per la Dashboard Freccette - Progetto BitPub.
 * Implementa le logiche di networking asincrono, gestione link HATEOAS
 * e visualizzazione statistiche aggregate.
 * Gestisce rigorosamente il rinfresco della UI tramite il JavaFX Application Thread.
 *
 * @author Timothy Giolito
 */
public class FreccetteDashboardController {

    // --- Costanti di Configurazione ---
    // Estrarre questi valori evita stringhe "magiche" sparse nel codice e centralizza le modifiche.
    private static final String API_ENDPOINT_STATISTICHE = "/statistiche/freccette";
    private static final String TESTO_CARICAMENTO = "...";
    private static final String TESTO_ERRORE = "N/D";
    private static final int PUNTEGGIO_INIZIALE = 501;

    // --- Componenti dell'interfaccia collegate tramite FXML ---
    @FXML public TableView<PartitaFreccette> tabellaPartite;
    @FXML public Button bottoneAggiorna;
    @FXML public Label statoLabel;
    
    @FXML public Label lblGiocatore1;
    @FXML public Label lblPunteggio1;
    @FXML public ProgressBar progressGiocatore1;

    @FXML public Label labelTotale180;
    @FXML public Label labelMediaPunti;

    // Servizio REST per le chiamate HTTP
    private final RestClient restClient;

    /**
     * Costruttore del controller. 
     * Inizializza i servizi necessari prima del caricamento della vista.
     */
    public FreccetteDashboardController() {
        this.restClient = RestClient.getInstance();
    }

    /**
     * Metodo di inizializzazione richiamato automaticamente da JavaFX.
     * Prepara lo stato iniziale della UI.
     */
    @FXML
    public void initialize() {
        resetUI();
        caricaStatisticheFreccette();
    }

    /**
     * Imposta la UI allo stato iniziale per prepararsi a una nuova partita o caricamento.
     */
    private void resetUI() {
        lblPunteggio1.setText(String.valueOf(PUNTEGGIO_INIZIALE));
        progressGiocatore1.setProgress(1.0);
        statoLabel.setText("Pronto.");
    }

    /**
     * Esegue una chiamata asincrona per recuperare le statistiche aggiornate dal server.
     * Garantisce la sicurezza dei thread durante l'aggiornamento dei nodi grafici.
     */
    @FXML
    public void caricaStatisticheFreccette() {
        // Messa in sicurezza: Platform.runLater assicura che modifichiamo i nodi 
        // grafici ESCLUSIVAMENTE dal JavaFX Application Thread, evitando eccezioni.
        Platform.runLater(() -> {
            labelTotale180.setText(TESTO_CARICAMENTO);
            labelMediaPunti.setText(TESTO_CARICAMENTO);
            statoLabel.setText("Recupero dati dal server in corso...");
            bottoneAggiorna.setDisable(true); // Previene chiamate multiple involontarie
        });

        // Esecuzione chiamata di rete su un thread asincrono separato
        restClient.faiChiamataGet(API_ENDPOINT_STATISTICHE, StatisticheFreccette.class)
                .thenAccept(statistiche -> {
                    // Riversamento risultati dal thread di rete al thread grafico
                    Platform.runLater(() -> {
                        labelTotale180.setText(String.valueOf(statistiche.getTotale180()));
                        // Formattazione a due decimali per una lettura più pulita
                        labelMediaPunti.setText(String.format("%.2f", statistiche.getMediaPuntiTorneo()));
                        statoLabel.setText("Dati aggiornati con successo.");
                        bottoneAggiorna.setDisable(false);
                    });
                })
                .exceptionally(errore -> {
                    // Controllo integrità per evitare crash. Notifichiamo l'utente dell'errore.
                    Platform.runLater(() -> {
                        labelTotale180.setText(TESTO_ERRORE);
                        labelMediaPunti.setText(TESTO_ERRORE);
                        statoLabel.setText("Errore di rete: impossibile recuperare le statistiche.");
                        bottoneAggiorna.setDisable(false);
                    });
                    return null; // Necessario per la firma di exceptionally
                });
    }
}