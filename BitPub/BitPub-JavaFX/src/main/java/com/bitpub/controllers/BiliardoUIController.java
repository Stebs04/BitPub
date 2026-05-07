package com.bitpub.controllers;

import com.bitpub.model.BiliardoStatistiche;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;

/**
 * Controller responsabile della gestione dell'interfaccia grafica per le statistiche del Biliardo.
 * Implementa un'architettura basata su client passivo e reattivo, sfruttando il paradigma HATEOAS
 * per disaccoppiare l'interfaccia dagli URI fisici del backend. Le rotte di comunicazione
 * non sono hardcoded ma scoperte dinamicamente a runtime interrogando le risorse root.
 *
 * @author Luca Franzon
 */
public class BiliardoUIController {

    // Componenti dell'interfaccia utente mappati tramite FXML per l'esposizione dei dati
    @FXML private Label serieMassimaLabel;
    @FXML private ListView<String> storicoListView;
    @FXML private Label statoLabel;
    @FXML private Button btnAggiorna;

    // Istanza singleton del client HTTP dedicata alla gestione delle richieste di rete
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Metodo di callback invocato automaticamente dal framework JavaFX al termine
     * del parsing del file FXML. Innesca immediatamente la catena ipermediale
     * per il popolamento iniziale della vista.
     */
    @FXML
    public void initialize() {
        caricaDatiBiliardo();
    }

    /**
     * Coordina il recupero asincrono delle statistiche aggiornate dal backend cloud.
     * Applica il pattern di discovery HATEOAS e assicura che ogni mutazione del DOM
     * grafico avvenga rigorosamente sul JavaFX Application Thread per prevenire
     * eccezioni di concorrenza.
     */
    @FXML
    public void caricaDatiBiliardo() {
        // Blocca le interazioni utente concorrenti e fornisce un feedback visivo immediato dello stato di attesa
        Platform.runLater(() -> {
            btnAggiorna.setDisable(true);
            statoLabel.setText("Scoperta risorse biliardo...");
        });

        // Avvia la fase di discovery interrogando la root dell'API per localizzare i servizi esposti
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                // Valida l'effettiva presenza dell'endpoint dedicato al biliardo prima di procedere
                if (root.getLinks() == null || !root.getLinks().containsKey("billiards-stats")) {
                    throw new RuntimeException("Risorsa 'billiards-stats' non trovata nei metadati Root.");
                }
                
                String statsUrl = root.getLinks().get("billiards-stats").getHref();
                
                // Concatena la chiamata di fetch dati puntando direttamente all'URI appena scoperto
                return restClient.getAsync(statsUrl, BiliardoStatistiche.class);
            })
            .thenAccept(statistiche -> {
                // Delega il rendering dei risultati formattati al thread grafico principale
                Platform.runLater(() -> {
                    if (statistiche != null) {
                        serieMassimaLabel.setText(String.valueOf(statistiche.getSerieMassimaPalle()));
                        
                        // Esegue un aggiornamento atomico dell'ObservableList sottostante per evitare sfarfallii visivi
                        storicoListView.getItems().setAll(statistiche.getStoricoPartite());
                        
                        statoLabel.setText("Dati biliardo aggiornati.");
                    } else {
                        statoLabel.setText("Nessun dato disponibile.");
                    }
                    btnAggiorna.setDisable(false);
                });
            })
            .exceptionally(ex -> {
                // Intercetta le anomalie di rete o di parsing, garantendo il ripristino dell'interattività UI
                Platform.runLater(() -> {
                    statoLabel.setText("Errore: " + ex.getMessage());
                    btnAggiorna.setDisable(false);
                    System.err.println("[BiliardoUI] Errore asincrono: " + ex.getMessage());
                });
                return null;
            });
    }
}