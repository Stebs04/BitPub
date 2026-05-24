package com.bitpub.controllers;

import com.bitpub.models.*;
import com.bitpub.models.Torneo.TipoGioco;
import com.bitpub.models.Torneo.ModalitaTorneo;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller responsabile della gestione della dashboard dedicata al ruolo Gestore.
 * Implementa un approccio architetturale basato su client passivo e reattivo,
 * delegando la scoperta degli endpoint al backend tramite il paradigma HATEOAS.
 * Sfrutta le Timeline di JavaFX per garantire un polling dei dati sicuro
 * rispetto al thread dell'interfaccia grafica, evitando colli di bottiglia e memory leak.
 *
 * @author Stefano Bellan 20054330
 */
public class GestoreDashboardController {

    // Componenti UI per la visualizzazione dello stato delle macchine fisiche
    @FXML private TableView<Macchina> macchineTable;
    @FXML private TableColumn<Macchina, String> colMacchinaNome, colMacchinaTipo, colMacchinaStato;
    
    // Componenti UI per la visualizzazione delle partite correntemente in esecuzione
    @FXML private TableView<Partita> partiteTable;
    @FXML private TableColumn<Partita, String> colPartitaTipo, colPartitaGiocatori, colPartitaInizio;

    // Componenti UI per la sezione statistica della dashboard
    @FXML private PieChart tipoGiocoChart;
    @FXML private Label lblPartiteOggi, lblDurataMedia;
    @FXML private Tab tabStatistiche;

    // Componenti UI dedicati al form di creazione di un nuovo torneo
    @FXML private TextField txtNomeTorneo;
    @FXML private ChoiceBox<TipoGioco> choiceTipoGioco;
    @FXML private DatePicker dateInizio;
    @FXML private Spinner<Integer> spinnerPartecipanti;
    @FXML private ChoiceBox<ModalitaTorneo> choiceModalita;

    // Elementi di feedback visivo e grafici avanzati
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private BarChart<String, Number> barChartPartite;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // Gestore del ciclo di aggiornamento periodico e client di rete singleton
    private Timeline pollingTimeline;
    private final RestClient restClient = RestClient.getInstance();

    /**
     * DTO interno utilizzato per rappresentare in memoria lo stato e le informazioni
     * di una singola macchina di gioco, semplificando il binding con la TableView.
     */
    public static class Macchina {
        private Long id;
        private String nome;
        private String tipoGioco;
        private boolean attiva;
        private boolean attuatoreSbloccato;

        public String getNome() { return nome; }
        public String getTipoGioco() { return tipoGioco; }
        public boolean isAttiva() { return attiva; }
        public boolean isAttuatoreSbloccato() { return attuatoreSbloccato; }
    }

    /**
     * DTO utilizzato per mappare il payload JSON delle statistiche aggregate
     * fornito dal backend. I nomi delle variabili devono corrispondere al JSON del Cloud.
     */
    public static class StatisticheDTO {
        private Long localeId;
        private int partiteTotali;
        private double percentualeVittorieRossi;
        private double percentualeVittorieBlu;
        private double durataMediaMinuti;
        private int totaleRullate;

        public int getPartiteTotali() { return partiteTotali; }
        public double getDurataMediaMinuti() { return durataMediaMinuti; }
        public double getPercentualeVittorieRossi() { return percentualeVittorieRossi; }
        public double getPercentualeVittorieBlu() { return percentualeVittorieBlu; }
        public int getTotaleRullate() { return totaleRullate; }
    }

    /**
     * Metodo di callback invocato dal framework JavaFX al termine del caricamento del file FXML.
     * Si occupa di inizializzare i binding dei componenti, configurare il sistema di polling
     * e registrare i listener per il caricamento pigro dei dati.
     */
    @FXML
    public void initialize() {
        if (macchineTable != null) {
            setupTables();
        }
        if (txtNomeTorneo != null) {
            setupForm();
        }

        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> pollData()));
        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();

        // Forza la prima estrazione dei dati in modo da popolare la dashboard istantaneamente
        pollData();

        // Listener che implementa il lazy loading delle statistiche, effettuando chiamate di rete
        // solo nel momento in cui l'operatore seleziona effettivamente la scheda dedicata
        if (tabStatistiche != null) {
            tabStatistiche.setOnSelectionChanged(event -> {
                if (tabStatistiche.isSelected()) {
                    loadStatistics();
                }
            });
        }
    }

    @FXML
    public void refreshStats() {
        loadStatistics();
    }

    /**
     * Configura il mapping tra le proprietà dei DTO e le rispettive colonne delle tabelle.
     * Impiega PropertyValueFactory per un binding riflettivo diretto o lambda expression
     * laddove sia necessaria una trasformazione del dato (es. rendering dello stato booleano).
     */
    private void setupTables() {
        colMacchinaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colMacchinaTipo.setCellValueFactory(new PropertyValueFactory<>("tipoGioco"));

        colMacchinaStato.setCellValueFactory(cellData -> {
            Macchina m = cellData.getValue();
            String statoRete = m.isAttiva() ? "🟢 ONLINE" : "🔴 OFFLINE";
            String statoAttuatore = m.isAttuatoreSbloccato() ? "🔓 SBLOCCATO" : "🔒 BLOCCATO";
            return new javafx.beans.property.SimpleStringProperty(statoRete + " | " + statoAttuatore);
        });

        colPartitaTipo.setCellValueFactory(new PropertyValueFactory<>("tipoGioco"));
        colPartitaGiocatori.setCellValueFactory(new PropertyValueFactory<>("nomiGiocatori"));
        colPartitaInizio.setCellValueFactory(new PropertyValueFactory<>("timestampInizio"));
    }

    /**
     * Inizializza i componenti del form per la creazione di un torneo definendo
     * i domini di valori ammessi (enum), i limiti degli spinner e le date di default.
     */
    private void setupForm() {
        choiceTipoGioco.setItems(FXCollections.observableArrayList(TipoGioco.values()));
        choiceModalita.setItems(FXCollections.observableArrayList(ModalitaTorneo.values()));
        spinnerPartecipanti.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 64, 8));
        dateInizio.setValue(LocalDate.now().plusDays(1));
    }

    /**
     * Gestisce il recupero asincrono dei dati per alimentare le viste principali della dashboard.
     * Implementa la navigazione HATEOAS interrogando prima l'endpoint radice per scoprire
     * gli URI delle risorse, e successivamente parallelizza le chiamate di dettaglio.
     */
    private void pollData() {
        // DISCOVERY: Interroga la root API per identificare i percorsi esposti dinamicamente
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                // Estrazione dei link specifici per il profilo gestore
                String macchineUrl = root.getLinks().get("gestore-macchine").getHref();
                String partiteUrl = root.getLinks().get("gestore-partite-attive").getHref();

                // Lancia le richieste di fetch per macchine e partite in modo concorrente
                // Ora richiediamo direttamente un array di oggetti Macchina deserializzati
                CompletableFuture<Macchina[]> fMacchine = restClient.getAsync(macchineUrl, Macchina[].class);
                CompletableFuture<Partita[]> fPartite = restClient.getAsync(partiteUrl, Partita[].class);

                return fMacchine.thenCombine(fPartite, (macchineArray, partite) -> {
                    // Trasforma l'array JSON deserializzato in una Lista Java
                    List<Macchina> listaMacchine = java.util.Arrays.asList(macchineArray);
                    return new Object[]{listaMacchine, partite};
                });
            })
            .thenAccept(risultatiCombinati -> {
                @SuppressWarnings("unchecked")
                List<Macchina> listaMacchine = (List<Macchina>) risultatiCombinati[0];
                Partita[] partiteAttive = (Partita[]) risultatiCombinati[1];

                Platform.runLater(() -> {
                    macchineTable.setItems(FXCollections.observableArrayList(listaMacchine));
                    partiteTable.setItems(FXCollections.observableArrayList(partiteAttive));
                });
            })
            .exceptionally(ex -> {
                // Intercettazione silenziosa dell'errore di polling per non bloccare il ciclo operativo
                System.err.println("[GestoreDashboard] Errore nel polling: " + ex.getMessage());
                return null;
            });
    }

    /**
     * Recupera dal backend il set di dati analitici e popola il pannello statistiche.
     * Sfrutta HATEOAS: chiede alla root API dove si trovano le statistiche.
     */
    private void loadStatistics(){
        Platform.runLater(() -> loadingIndicator.setVisible(true));

        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
                .thenCompose(root -> {
                    //Il cloud ci fornisce L'URL già compilato con l'ID del locale Corretto
                    String statsUrl = root.getLinks().get("gestore-statistiche").getHref();
                    return restClient.getAsync(statsUrl, StatisticheDTO.class);
                })
                .thenAccept(stats -> {
                    Platform.runLater(() -> {
                        if(stats != null)  {
                            lblPartiteOggi.setText("Partite Totali: " + stats.getPartiteTotali());
                            lblDurataMedia.setText("Durata media: " + stats.getDurataMediaMinuti() + " min");

                            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                                    new PieChart.Data("Vittorie Rossi (%)", stats.getPercentualeVittorieRossi()),
                                    new PieChart.Data("Vittorie Blu (%)", stats.getPercentualeVittorieBlu())
                            );
                            tipoGiocoChart.setData(pieData);
                        }
                        loadingIndicator.setVisible(false);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        loadingIndicator.setVisible(false);
                        showAlert(Alert.AlertType.WARNING, "Dati non disponibili", "Impossibile caricare le statistiche.");
                        System.err.println("[GestoreDashboard] Errore stats: " + ex.getMessage());
                    });
                    return null;
                });
    }

    /**
     * Raccoglie, valida e trasmette le informazioni del form per registrare un nuovo torneo.
     * Il processo è integralmente asincrono e prevede feedback visivi in caso di successo o errore.
     */
    @FXML
    public void handleCreaTorneo() {
        // Validazione preventiva lato client per bloccare richieste palesemente malformate
        if (txtNomeTorneo.getText() == null || txtNomeTorneo.getText().trim().isEmpty() ||
                choiceTipoGioco.getValue() == null ||
                choiceModalita.getValue() == null ||
                dateInizio.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Dati Mancanti", "Compila tutti i campi prima di creare il torneo!");
            return;
        }

        // Preparazione dell'oggetto di dominio da serializzare come payload JSON
        Torneo nuovoTorneo = new Torneo();
        nuovoTorneo.setNome(txtNomeTorneo.getText().trim());
        nuovoTorneo.setTipoGioco(choiceTipoGioco.getValue());
        nuovoTorneo.setDataInizio(dateInizio.getValue());
        nuovoTorneo.setMaxPartecipanti(spinnerPartecipanti.getValue());
        nuovoTorneo.setModalita(choiceModalita.getValue());

        Platform.runLater(() -> loadingIndicator.setVisible(true));

        // DISCOVERY: Localizza l'endpoint preposto all'inserimento delle risorse di tipo torneo
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String torneiUrl = root.getLinks().get("tornei").getHref();
                return restClient.postAsync(torneiUrl, nuovoTorneo, Torneo.class);
            })
            .thenAccept(res -> {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    showAlert(Alert.AlertType.INFORMATION, "Successo", "Torneo creato correttamente!");
                    resetForm();
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    showAlert(Alert.AlertType.ERROR, "Errore di Rete", "Impossibile creare il torneo: " + ex.getMessage());
                });
                return null;
            });
    }

    /**
     * Ripristina il form di creazione torneo allo stato iniziale dopo un inserimento o annullamento.
     */
    private void resetForm() {
        txtNomeTorneo.clear();
        dateInizio.setValue(LocalDate.now().plusDays(1));
    }

    /**
     * Utility metod per generare ed esporre finestre di dialogo all'operatore in maniera unificata.
     *
     * @param type La gravità o tipologia del messaggio (es. ERROR, WARNING, INFO)
     * @param title L'intestazione della finestra di dialogo
     * @param content Il corpo testuale del messaggio
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Permette l'interruzione pulita del ciclo di aggiornamento.
     * Deve essere invocata dai livelli superiori durante il ciclo di distruzione della vista
     * o durante la navigazione in uscita per scongiurare leak di memoria e calcoli inutili in background.
     */
    public void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }
    }

    /**
     * Gestisce l'azione di logout dell'utente.
     * Ferma il polling dei dati, pulisce la sessione e reindirizza alla schermata di login.
     */
    @FXML
    public void handleLogout() {
        System.out.println("[GestoreDashboard] Inizio procedura di logout...");

        // 1. Fermiamo il ciclo continuo di aggiornamento per non sprecare risorse
        stopPolling();

        // 2. Puliamo la sessione (token, ruolo, ecc.) usando il Singleton
        com.bitpub.network.SessionManager.getInstance().logout();

        // 3. Diciamo alla classe Main di riportarci alla schermata iniziale
        com.bitpub.Main.eseguiLogout();
    }
}