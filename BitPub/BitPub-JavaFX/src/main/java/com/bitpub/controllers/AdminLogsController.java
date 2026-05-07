package com.bitpub.controllers;

import com.bitpub.models.SystemLog;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;

/**
 * Controller dedicato al monitoraggio e filtraggio dei log di sistema all'interno della dashboard amministrativa.
 * L'architettura è stata progettata seguendo il pattern del client passivo e reattivo: 
 * il frontend non possiede alcuna conoscenza pregressa degli endpoint (zero hardcoding), 
 * ma naviga l'albero delle risorse esposto dal backend tramite il paradigma HATEOAS.
 * Questa scelta architetturale garantisce un totale disaccoppiamento e permette di modificare
 * le rotte lato server senza richiedere alcuna ricompilazione del client, difendendo
 * in modo rigoroso il principio di separazione delle responsabilità (Separation of Concerns).
 *
 * @author Stefano Bellan 20054330
 */
public class AdminLogsController {

    // Componenti dell'interfaccia legati al rendering tabellare dei log
    @FXML private TableView<SystemLog> logsTable;
    @FXML private TableColumn<SystemLog, String> colTimestamp;
    @FXML private TableColumn<SystemLog, String> colLevel;
    @FXML private TableColumn<SystemLog, String> colSource;
    @FXML private TableColumn<SystemLog, String> colMessage;
    
    // Controlli per la segmentazione e l'analisi dei log in base al livello di gravità
    @FXML private ComboBox<String> filterLevelCombo;

    // Struttura dati reattiva che fa da ponte tra la memoria del client e il DOM di JavaFX
    private final ObservableList<SystemLog> masterData = FXCollections.observableArrayList();
    
    // Gestore centralizzato per il ciclo di vita delle connessioni HTTP
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Hook di inizializzazione invocato dal framework JavaFX al termine del caricamento della vista.
     * Si occupa di legare le proprietà del modello di dominio SystemLog alle colonne della tabella,
     * applicare misure di programmazione difensiva sull'interfaccia di filtro e innescare 
     * il popolamento asincrono iniziale.
     */
    @FXML
    public void initialize() {
        // Configurazione del binding tramite riflessione sulle proprietà della classe SystemLog
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLevel.setCellValueFactory(new PropertyValueFactory<>("level"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));

        logsTable.setItems(masterData);

        // Inizializzazione sicura della combobox come meccanismo di fallback qualora il file FXML non definisca i nodi
        if (filterLevelCombo.getItems().isEmpty()) {
            filterLevelCombo.setItems(FXCollections.observableArrayList("ALL", "INFO", "WARN", "ERROR", "DEBUG"));
        }
        filterLevelCombo.setValue("ALL");

        // Trigger immediato per la valorizzazione del cruscotto all'apertura
        refreshLogs();
    }

    /**
     * Orchesta il ciclo di recupero e filtraggio dei log dal cloud.
     * Il metodo applica un flusso strettamente asincrono composto da tre fasi:
     * 1. Discovery: interroga la Root API per individuare l'indirizzo della risorsa.
     * 2. Fetch: appende dinamicamente i parametri di query in base al filtro di severity.
     * 3. Render: muta il DOM grafico riversando i dati estratti esclusivamente sul JavaFX Application Thread.
     */
    @FXML
    public void refreshLogs() {
        String level = filterLevelCombo.getValue();

        // Sincronizzazione preventiva del DOM grafico per informare l'operatore dell'avvio della transazione di rete
        Platform.runLater(() -> logsTable.setPlaceholder(new Label("Ricerca log di sistema in corso...")));

        // Fase di discovery: navigazione dell'albero HATEOAS partendo dall'entry point noto dell'API
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                // Verifica dell'integrità del contratto ipermediale esposto dal server
                if (root.getLinks() == null || !root.getLinks().containsKey("system-logs")) {
                    throw new RuntimeException("Link 'system-logs' non esposto dal server.");
                }

                String logsUrl = root.getLinks().get("system-logs").getHref();

                // Costruzione parametrica della query string rispettando lo standard REST per il filtraggio delle collezioni
                if (level != null && !"ALL".equals(level)) {
                    logsUrl += "?level=" + level;
                }

                // Inoltro della richiesta GET verso l'URI finalizzato
                return restClient.getAsync(logsUrl, SystemLog[].class);
            })
            .thenAccept(logArray -> {
                // Sostituzione atomica del contenuto della ObservableList per ottimizzare i cicli di render della TableView
                Platform.runLater(() -> {
                    if (logArray != null && logArray.length > 0) {
                        masterData.setAll(Arrays.asList(logArray));
                    } else {
                        masterData.clear();
                        logsTable.setPlaceholder(new Label("Nessun log trovato per il filtro selezionato."));
                    }
                });
            })
            .exceptionally(ex -> {
                // Gestione strutturata degli errori di trasporto per prevenire interruzioni silenziose dell'applicazione
                Platform.runLater(() -> {
                    masterData.clear();
                    logsTable.setPlaceholder(new Label("Errore di rete: Impossibile caricare i log."));
                    System.err.println("[AdminLogsController] Errore caricamento log: " + ex.getMessage());
                });
                return null;
            });
    }
}