package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.model.StatisticheCalciobalilla;
import com.bitpub.models.Torneo;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import com.bitpub.network.SessionContext;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;

/**
 * Controller responsabile della gestione dell'area dedicata al calciobalilla lato utente.
 * Agisce come hub centrale per la navigazione ipermediale tra la sezione delle partite libere 
 * e quella dei tornei organizzati. Implementa una logica totalmente asincrona e non bloccante,
 * guidata dai link scoperti a runtime tramite il paradigma HATEOAS per garantire un disaccoppiamento 
 * totale dalle rotte hardcoded del backend.
 *
 * @author Stefano Bellan 20054330
 */
public class CalciobalillaUtenteController {

    // Componenti dell'interfaccia dedicati al riepilogo delle prestazioni personali
    @FXML private Label lblWinLoss, lblGolFatti, lblGolSubiti;
    @FXML private Button btnGiocaOra;

    // Componenti dell'interfaccia per la consultazione e l'iscrizione alle competizioni
    @FXML private TableView<Torneo> tableTornei;
    @FXML private TableColumn<Torneo, String> colNomeTorneo, colDataTorneo, colStatoTorneo;
    @FXML private TextField txtNomeSquadra;

    // Istanza singleton del client HTTP per le comunicazioni di rete
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Metodo di callback standard del ciclo di vita di JavaFX.
     * Configura il data binding della tabella e innesca il fetch iniziale dei dati
     * necessari a popolare la schermata appena l'albero della scena è pronto.
     */
    @FXML
    public void initialize() {
        setupTable();
        caricaDatiIniziali();
    }

    /**
     * Inizializza le colonne della tabella definendo le factory per l'estrazione delle proprietà.
     * Associa i campi dell'oggetto Torneo alle rispettive colonne visive.
     */
    private void setupTable() {
        colNomeTorneo.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colDataTorneo.setCellValueFactory(new PropertyValueFactory<>("dataInizio"));
        colStatoTorneo.setCellValueFactory(new PropertyValueFactory<>("modalita"));
    }

    /**
     * Raggruppa le chiamate di inizializzazione lanciando il caricamento delle statistiche
     * e della lista tornei in parallelo, massimizzando l'efficienza della fase di avvio.
     */
    private void caricaDatiIniziali() {
        caricaStatistichePartite();
        caricaListaTornei();
    }

    // =========================================================================
    // AREA PARTITE: LOGICA REATTIVA
    // =========================================================================

    /**
     * Gestisce la richiesta dell'utente di avviare una nuova partita amichevole.
     * Attua un blocco preventivo dell'interfaccia per evitare richieste multiple,
     * scopre dinamicamente l'endpoint di avvio e istanzia una nuova sessione sul server.
     *
     * @param event L'evento di click catturato dall'interfaccia grafica
     */
    @FXML
    void handleGiocaPartita(ActionEvent event) {
        // Disabilitazione immediata del pulsante per impedire interazioni concorrenti
        btnGiocaOra.setDisable(true);
        btnGiocaOra.setText("Inizializzazione...");

        // Interroga la root API per localizzare l'indirizzo operativo dedicato all'avvio sessione
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String startUrl = root.getLinks().get("foosball-start").getHref();
                JsonObject payload = new JsonObject();
                payload.addProperty("tipo", "AMICHEVOLE");
                
                // Propaga la chiamata POST verso l'endpoint appena scoperto
                return restClient.postAsync(startUrl, payload, JsonObject.class);
            })
            .thenAccept(session -> {
                // Registra l'ID generato dal backend nel contesto globale per l'uso da parte del tabellone
                SessionContext.setCurrentSessionId(session.get("id").getAsLong());
                
                // Delega il cambio di scena al thread grafico principale
                Platform.runLater(() -> Main.navigaVerso("/FoosballScoreboard.fxml", "BitPub - Match Live"));
            })
            .exceptionally(ex -> {
                // Procedura di ripristino dell'interfaccia in caso di fallimento della chiamata di rete
                Platform.runLater(() -> {
                    btnGiocaOra.setDisable(false);
                    btnGiocaOra.setText("Gioca Partita");
                    new Alert(Alert.AlertType.ERROR, "Errore avvio: " + ex.getMessage()).show();
                });
                return null;
            });
    }

    /**
     * Effettua il recupero asincrono dello storico prestazioni dell'utente.
     * Aggiorna le label della dashboard con il conteggio vittorie/sconfitte e le reti totali.
     */
    private void caricaStatistichePartite() {
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String statsUrl = root.getLinks().get("foosball-personal-stats").getHref();
                return restClient.getAsync(statsUrl, StatisticheCalciobalilla.class);
            })
            .thenAccept(stats -> Platform.runLater(() -> {
                lblWinLoss.setText(stats.getVinte() + " / " + stats.getPerse());
                lblGolFatti.setText(String.valueOf(stats.getGolFatti()));
                lblGolSubiti.setText(String.valueOf(stats.getGolSubiti()));
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> lblWinLoss.setText("Dati non disponibili"));
                return null;
            });
    }

    // =========================================================================
    // AREA TORNEI: LOGICA CRUD/DISCOVERY
    // =========================================================================

    /**
     * Interroga l'API per ottenere l'elenco dei tornei di calciobalilla programmati.
     * I dati recuperati vengono convertiti in una ObservableList per il render nella TableView.
     */
    private void caricaListaTornei() {
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String torneiUrl = root.getLinks().get("tornei-calciobalilla").getHref();
                return restClient.getAsync(torneiUrl, Torneo[].class);
            })
            .thenAccept(tornei -> Platform.runLater(() -> 
                tableTornei.setItems(FXCollections.observableArrayList(Arrays.asList(tornei)))
            ));
    }

    /**
     * Gestisce la logica di iscrizione della squadra a un torneo specifico.
     * Valida l'input locale e verifica la presenza del link operativo direttamente 
     * all'interno del DTO del torneo, confermando l'approccio HATEOAS a livello di entità.
     */
    @FXML
    void handleIscrizioneTorneo() {
        Torneo selezionato = tableTornei.getSelectionModel().getSelectedItem();
        String nomeSquadra = txtNomeSquadra.getText().trim();

        // Controllo di coerenza sui dati forniti prima di impegnare la rete
        if (selezionato == null || nomeSquadra.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Seleziona un torneo e inserisci il nome della squadra").show();
            return;
        }

        // Verifica che il backend abbia esposto l'azione di iscrizione per l'elemento selezionato
        if (selezionato.getLinks().containsKey("iscriviti")) {
            String iscrizioneUrl = selezionato.getLinks().get("iscriviti").getHref();
            
            JsonObject payload = new JsonObject();
            payload.addProperty("nomeSquadra", nomeSquadra);

            // Sottomette i dati di iscrizione e aggiorna la tabella per riflettere le modifiche lato server
            restClient.postAsync(iscrizioneUrl, payload, JsonObject.class)
                .thenAccept(res -> Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "Iscrizione completata!").show();
                    caricaListaTornei();
                }));
        }
    }

    /**
     * Fornisce il comando di uscita dalla vista corrente per tornare alla dashboard principale.
     *
     * @param event L'evento scatenato dal pulsante di ritorno
     */
    @FXML
    void tornaAllaDashboard(ActionEvent event) {
        Main.navigaVerso("/DashboardView.fxml", "BitPub - Dashboard");
    }
}