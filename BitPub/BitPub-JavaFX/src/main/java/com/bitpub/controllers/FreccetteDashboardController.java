package com.bitpub.controllers;

import com.bitpub.models.PartitaFreccette;
import com.bitpub.models.StatisticheFreccette;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;

/**
 * Controller per la Dashboard Freccette del progetto BitPub.
 * Responsabile della gestione dell'interfaccia utente per il monitoraggio
 * delle partite e delle statistiche associate al gioco delle freccette.
 *
 * Implementa un'architettura a client passivo, demandando la scoperta degli 
 * endpoint operativi al paradigma HATEOAS esposto dal backend. Tutte le
 * modifiche all'interfaccia utente sono confinate all'Application Thread
 * di JavaFX per garantire la stabilità grafica.
 *
 * @author Timothy Giolito
 */
public class FreccetteDashboardController {

    // Placeholder testuali per la gestione degli stati di transizione dell'interfaccia.
    private static final String TESTO_CARICAMENTO = "...";
    private static final String TESTO_ERRORE = "N/D";

    @FXML public TableView<PartitaFreccette> tabellaPartite;
    @FXML public Button bottoneAggiorna;
    @FXML public Label statoLabel;
    
    @FXML public Label lblGiocatore1;
    @FXML public Label lblPunteggio1;
    
    @FXML public Label labelTotale180;
    @FXML public Label labelMediaPunti;

    // Istanza unica del client REST per le comunicazioni HTTP col backend.
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Inizializzazione del controller invocata automaticamente da JavaFX.
     * Avvia immediatamente il processo asincrono di caricamento dati.
     */
    @FXML
    public void initialize() {
        aggiornaStatistiche();
    }

    /**
     * Avvia la sequenza di aggiornamento dei dati statistici dalla rete.
     * Mette in sicurezza i componenti visivi prima della chiamata asincrona,
     * sfrutta il discovery HATEOAS per recuperare la risorsa esatta, e gestisce
     * il rilascio dei nuovi valori (o degli errori) sul thread principale UI.
     */
    @FXML
    public void aggiornaStatistiche() {
        // Prepara l'interfaccia per mostrare all'utente che un caricamento è in corso, 
        // inibendo ulteriori interazioni concorrenti sul bottone.
        Platform.runLater(() -> {
            statoLabel.setText("Ricerca statistiche in corso...");
            labelTotale180.setText(TESTO_CARICAMENTO);
            labelMediaPunti.setText(TESTO_CARICAMENTO);
            bottoneAggiorna.setDisable(true); 
        });

        // Avvia la catena di discovery asincrona partendo dalla root dell'API.
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
                .thenCompose(root -> {
                    // Controlla la presenza effettiva del link richiesto prima di tentare l'accesso.
                    if (root == null || root.getLinks() == null || !root.getLinks().containsKey("freccette-stats")) {
                        throw new RuntimeException("Endpoint freccette non disponibile nella Root HATEOAS.");
                    }
                    
                    String statsUrl = root.getLinks().get("freccette-stats").getHref();
                    
                    // Concatena la chiamata effettiva verso la risorsa statistica identificata.
                    return restClient.getAsync(statsUrl, StatisticheFreccette.class);
                })
                .thenAccept(statistiche -> {
                    // Elabora la risposta positiva allineando i dati sul thread UI di JavaFX.
                    Platform.runLater(() -> {
                        if (statistiche != null) {
                            labelTotale180.setText(String.valueOf(statistiche.getTotale180()));
                            // Applica una formattazione a due decimali per mantenere l'interfaccia ordinata.
                            labelMediaPunti.setText(String.format("%.2f", statistiche.getMediaPuntiTorneo()));
                            statoLabel.setText("Dati aggiornati con successo.");
                        } else {
                            statoLabel.setText("Nessun dato statistico disponibile.");
                        }
                        bottoneAggiorna.setDisable(false);
                    });
                })
                .exceptionally(errore -> {
                    // Gestisce i fallimenti a qualsiasi livello della catena (discovery o fetch dati),
                    // notificando l'utente e ripristinando lo stato interattivo.
                    Platform.runLater(() -> {
                        labelTotale180.setText(TESTO_ERRORE);
                        labelMediaPunti.setText(TESTO_ERRORE);
                        statoLabel.setText("Errore di rete: impossibile recuperare le statistiche.");
                        bottoneAggiorna.setDisable(false);
                        System.err.println("[FreccetteDashboard] " + errore.getMessage());
                    });
                    return null; 
                });
    }
}