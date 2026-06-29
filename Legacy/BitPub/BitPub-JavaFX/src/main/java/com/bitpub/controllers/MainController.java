package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller architetturale di base (Shell) per l'applicativo client BitPub.
 * Implementa la logica di una Single Page Application (SPA) in ambito desktop,
 * gestendo una sidebar di navigazione persistente e un'area di contenuto dinamica
 * in cui i singoli moduli FXML vengono iniettati ed espulsi a runtime.
 * Questa soluzione riduce il sovraccarico della memoria rispetto al mantenimento
 * di finestre multiple e centralizza la gestione dello stato visivo.
 * Il controller gestisce inoltre le interfacce con i sottosistemi hardware simulati,
 * istanziando processi paralleli a livello di sistema operativo per scongiurare
 * il blocco dell'interfaccia utente.
 *
 * @author Stefano Bellan 20054330
 */
public class MainController {

    // Contenitore radice per l'ancoraggio e lo swap delle viste modulari
    @FXML private StackPane contentArea;
    
    // Controlli di navigazione della barra laterale
    @FXML private Button btnDashboard;
    @FXML private Button btnCalciobalilla;
    @FXML private Button btnFreccette;
    @FXML private Button btnBiliardo;
    
    // Barra di stato inferiore per il feedback contestuale sulle operazioni in background
    @FXML private Label statusLabel;

    // Client HTTP per mantenere l'allineamento architetturale qualora la shell necessiti di dati di rete
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Entry-point del ciclo di vita generato dal framework JavaFX.
     * Forza il montaggio della dashboard di default al primo rendering della shell
     * per non presentare un'area di lavoro vuota all'operatore.
     */
    @FXML
    public void initialize() {
        mostraDashboard();
    }

    // =========================================================================
    // LOGICA DI NAVIGAZIONE (HATEOAS-Ready)
    // =========================================================================

    /**
     * Gestisce la transizione verso il cruscotto principale.
     * Delega l'aggiornamento stilistico del bottone e innesca la routine di iniezione.
     */
    @FXML
    public void mostraDashboard() {
        aggiornaStatoBottone(btnDashboard);
        caricaVistaDinamica("/DashboardView.fxml", "Dashboard Generale");
    }

    /**
     * Avvia il modulo dedicato al Calciobalilla.
     * Oltre al montaggio grafico della vista, orchestra la parallelizzazione dell'avvio 
     * del motore fisico simulato. Lo spawn del processo viene delegato a un worker thread
     * indipendente per preservare l'alta reattività del JavaFX Application Thread.
     */
    @FXML
    public void mostraCalciobalilla() {
        aggiornaStatoBottone(btnCalciobalilla);
        caricaVistaDinamica("/CalciobalillaUtenteView.fxml", "Area Calciobalilla");
        
        new Thread(this::avviaSimulatoreCalciobalilla).start();
    }

    /**
     * Attiva il contesto applicativo per la gestione del gioco delle freccette.
     */
    @FXML
    public void mostraFreccette() {
        aggiornaStatoBottone(btnFreccette);
        caricaVistaDinamica("/FreccetteDashboard.fxml", "Area Freccette");
    }

    /**
     * Attiva il contesto applicativo per la visualizzazione delle statistiche di biliardo.
     */
    @FXML
    public void mostraBiliardo() {
        aggiornaStatoBottone(btnBiliardo);
        caricaVistaDinamica("/BiliardoView.fxml", "Area Biliardo");
    }

    // =========================================================================
    // CORE ENGINE: CARICAMENTO E ANIMAZIONE
    // =========================================================================

    /**
     * Motore centrale per il context-switching delle interfacce.
     * Pulisce l'albero del DOM grafico, compila a runtime il nuovo file FXML richiesto
     * e applica un'interpolazione di opacità (FadeTransition) per addolcire il passaggio
     * visivo, riducendo il carico cognitivo dell'utente durante il cambio di contesto.
     *
     * @param fxmlPath Percorso del file descrittore della vista da renderizzare
     * @param titoloModulo Etichetta descrittiva proiettata nella barra di stato
     */
    private void caricaVistaDinamica(String fxmlPath, String titoloModulo) {
        Platform.runLater(() -> {
            try {
                // Feedback reattivo istantaneo sulla barra di sistema
                statusLabel.setText("Caricamento " + titoloModulo + "...");
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Node nuovaVista = loader.load();
                
                // Preparazione del nodo per l'animazione di dissolvenza in entrata
                nuovaVista.setOpacity(0);
                
                // Sostituzione atomica del contenuto precedente
                contentArea.getChildren().setAll(nuovaVista);
                
                // Configurazione ed esecuzione dell'interpolatore grafico a 400 millisecondi
                FadeTransition ft = new FadeTransition(Duration.millis(400), nuovaVista);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
                
                statusLabel.setText("Modulo attivo: " + titoloModulo);
                
            } catch (IOException e) {
                // Fallback di segnalazione in caso di risorsa FXML mancante o corrotta
                statusLabel.setText("Errore nel caricamento del modulo.");
                e.printStackTrace();
            }
        });
    }

    /**
     * Attiva l'infrastruttura di simulazione fisica interfacciandosi col sistema operativo.
     * Avvia un processo Maven isolato puntando alla root directory del modulo dei simulatori.
     * Questa separazione architetturale consente il testing eponimo delle dinamiche IoT 
     * senza accoppiare il demone fisico all'applicativo gestionale.
     */
    private void avviaSimulatoreCalciobalilla() {
        try {
            System.out.println("[Main] Avvio processo simulatore...");
            // Costruzione del comando shell per l'avvio della JVM dedicata al simulatore
            ProcessBuilder pb = new ProcessBuilder("mvn", "exec:java");
            pb.directory(new java.io.File("../BitPub-Simulators")); 
            pb.start();
        } catch (IOException e) {
            // Notifica sicura sul thread UI di un eventuale fallimento nello spawn del processo
            Platform.runLater(() -> statusLabel.setText("Avviso: Simulatore non avviato."));
            System.err.println("Impossibile avviare il simulatore: " + e.getMessage());
        }
    }

    /**
     * Manipola le classi pseudo-CSS dei controlli di navigazione per riflettere 
     * lo stato attivo corrente. Ripulisce preventivamente l'intera collezione 
     * e applica la marcatura esclusivamente al target selezionato.
     *
     * @param attivo Il controllo pulsante che deve ricevere il focus visivo
     */
    private void aggiornaStatoBottone(Button attivo) {
        Platform.runLater(() -> {
            btnDashboard.getStyleClass().remove("active");
            btnCalciobalilla.getStyleClass().remove("active");
            btnFreccette.getStyleClass().remove("active");
            btnBiliardo.getStyleClass().remove("active");
            
            attivo.getStyleClass().add("active");
        });
    }
}