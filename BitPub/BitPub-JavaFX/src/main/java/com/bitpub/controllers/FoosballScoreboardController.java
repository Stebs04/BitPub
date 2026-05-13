package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.network.SessionContext;
import com.bitpub.network.RispostaHateoas;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Controller responsabile del tabellone in tempo reale per il calciobalilla.
 * Implementa una logica dual-channel: da una parte riceve aggiornamenti push
 * ad alta frequenza tramite MQTT (eventi fisici dal tavolo e comandi Edge), 
 * dall'altra applica un meccanismo di polling reattivo HATEOAS verso il backend 
 * cloud come garanzia di stato (source of truth) e gestione del ciclo di vita.
 *
 * Le modifiche al DOM grafico sono sincronizzate sul thread UI per evitare 
 * eccezioni di concorrenza, e le risorse di rete vengono liberate esplicitamente 
 * alla chiusura per scongiurare memory leak.
 *
 * @author Stefano Bellan 20054330
 */
public class FoosballScoreboardController implements MqttCallback {

    // Etichette dedicate all'esposizione dei punteggi correnti.
    @FXML private Label lblScoreBlue;
    @FXML private Label lblScoreRed;
    
    // Elementi di notifica stato (es. In Attesa, Vittoria) e barra di progressione partita.
    @FXML private Label lblStatus;
    @FXML private ProgressBar progressWin;
    @FXML private Button btnBackToDashboard;

    // Soglia statica per determinare la fine dell'incontro.
    private static final int SCORE_TO_WIN = 10;

    // Motore per la ricezione asincrona degli eventi MQTT locali.
    private MqttClient mqttClient;
    
    // Client REST per interrogare il cloud in base alle specifiche HATEOAS.
    private final RestClient restClient = RestClient.getInstance();
    
    // Serializzatore/deserializzatore JSON.
    private final Gson gson = new Gson();

    // Timer JavaFX per schedulare le richieste REST senza bloccare la grafica.
    private Timeline pollingTimeline;
    
    // Flag di stato interno per ignorare pacchetti tardivi quando la partita è già conclusa.
    private boolean isFinished = false;

    /**
     * Entry-point del controller. Ripulisce la vista e avvia immediatamente
     * le due macchine di sincronizzazione dati: MQTT e Polling HTTP.
     */
    @FXML
    public void initialize() {
        System.out.println("[Scoreboard] Inizializzazione...");

        // Delega la formattazione iniziale al thread grafico principale.
        Platform.runLater(() -> {
            lblStatus.setText("IN ATTESA...");
            progressWin.setProgress(0.0);
        });

        setupMqttClient();
        startReactivePolling();
    }

    // =========================================================================
    // 1. CANALE MQTT (Aggiornamenti Push in Tempo Reale)
    // =========================================================================

    /**
     * Configura il client MQTT sottoscrivendosi sia al broker di campo (per i gol fisici)
     * che all'Edge Node (per il fine partita autoritativo). 
     * Impiega QoS 1 (At least once) per una ragionevole affidabilità della rete locale.
     */
    private void setupMqttClient() {
        try {
            String brokerUrl = "tcp://localhost:1883";
            String clientId = MqttClient.generateClientId();
            mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(5);

            mqttClient.setCallback(this);
            mqttClient.connect(options);
            System.out.println("[Scoreboard] MQTT Connesso.");

            // Sottoscrizione al topic di telemetria grezza prodotta dai sensori ottici del tavolo.
            mqttClient.subscribe("bitpub/locali/+/calciobalilla/+/eventi", 1);
            // Sottoscrizione al topic processato dall'Edge Node.
            mqttClient.subscribe("bitpub/edge/+/score", 1);

        } catch (MqttException e) {
            System.err.println("[Scoreboard] Errore avvio MQTT: " + e.getMessage());
            Platform.runLater(() -> lblStatus.setText("ERRORE MQTT LATERALE"));
        }
    }

    /**
     * Callback innescata dalla libreria Paho alla ricezione di un nuovo pacchetto.
     * Identifica l'origine tramite la struttura del topic ed estrae le metriche di interesse.
     *
     * @param topic L'indirizzo logico del messaggio in arrivo.
     * @param message Il payload grezzo, tipicamente in formato JSON.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Taglia l'elaborazione per non mostrare "gol fantasma" postumi.
        if (isFinished) return; 

        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(payload, JsonObject.class);

            int b = 0;
            int r = 0;

            if (topic.contains("/eventi")) {
                // Lettura dei gol diretti dai sensori.
                b = json.has("goalBlu") ? json.get("goalBlu").getAsInt() : 0;
                r = json.has("goalRossi") ? json.get("goalRossi").getAsInt() : 0;
            } else if (topic.contains("/score")) {
                // Lettura della rielaborazione dell'Edge.
                b = json.has("scoreBlue") ? json.get("scoreBlue").getAsInt() : 0;
                r = json.has("scoreRed") ? json.get("scoreRed").getAsInt() : 0;

                // Verifiche sull'integrità del ciclo di vita del torneo (fine per limite o forfait).
                if (json.has("status") && "FINISHED".equalsIgnoreCase(json.get("status").getAsString())) {
                    isFinished = true;
                    Platform.runLater(() -> {
                        lblStatus.setText("PARTITA TERMINATA (Edge)");
                        showBackButton();
                    });
                }
            }

            aggiornaUI(b, r);

        } catch (Exception e) {
            System.err.println("[Scoreboard] Errore parsing MQTT: " + e.getMessage());
        }
    }

    /**
     * Intercetta le cadute del bridge locale. Non termina l'applicazione,
     * poiché la ridondanza fornita dal polling REST può compensare il disservizio transitorio.
     */
    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[Scoreboard] MQTT Connessione persa: " + cause.getMessage());
        if (!isFinished) {
            Platform.runLater(() -> lblStatus.setText("MQTT OFFLINE - Polling attivo"));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Non utilizzato dal subscriber, ignorato per design.
    }

    // =========================================================================
    // 2. CANALE REST (Polling Reattivo HATEOAS / Fallback)
    // =========================================================================

    /**
     * Alloca la Timeline JavaFX per un'interrogazione HTTP circolare (ogni 3s).
     */
    private void startReactivePolling() {
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> pollStatus()));
        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();
    }

    /**
     * Naviga l'architettura HATEOAS per rintracciare lo stato assoluto della sessione nel Cloud.
     */
    private void pollStatus() {
        if (isFinished) return;

        String knownUrl = SessionContext.getCurrentSessionStatusUrl();
        CompletableFuture<String> sessionUrlFuture;

        // Valutazione rapida: riuso dell'endpoint noto vs discovery dalla Root.
        if (knownUrl != null && !knownUrl.isEmpty()) {
             sessionUrlFuture = CompletableFuture.completedFuture(knownUrl);
        } else {
             sessionUrlFuture = restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
                    .thenApply(root -> {
                        Long sid = SessionContext.getCurrentSessionId();
                        if (sid != null) {
                            // Tenta di usare il link specifico per il calciobalilla, altrimenti usa il generico sessions
                            String baseUrl = root.getLinks().containsKey("foosball-sessions") ? 
                                             root.getLinkSafe("foosball-sessions") : 
                                             root.getLinkSafe("sessions");
                            return baseUrl + "/" + sid;
                        }
                        throw new RuntimeException("URL Sessione non determinabile.");
                    });
        }

        // Chiamata REST all'endpoint isolato.
        sessionUrlFuture.thenCompose(url -> restClient.getAsync(url, JsonObject.class))
            .thenAccept(session -> {
                String status = session.has("status") ? session.get("status").getAsString() : "";
                
                int scoreB = session.has("scoreBlue") ? session.get("scoreBlue").getAsInt() : 0;
                int scoreR = session.has("scoreRed") ? session.get("scoreRed").getAsInt() : 0;
                
                aggiornaUI(scoreB, scoreR);

                // Controllo direttive autoritative inviate dall'interfaccia amministrativa cloud.
                if ("COMPLETED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                    isFinished = true;
                    Platform.runLater(() -> {
                        lblStatus.setText("CHIUSA DA REMOTO: " + status);
                        showBackButton();
                    });
                }
            })
            .exceptionally(ex -> {
                System.err.println("[Scoreboard] Errore Polling REST: " + ex.getMessage());
                return null;
            });
    }

    // =========================================================================
    // 3. AGGIORNAMENTO UI E NAVIGAZIONE
    // =========================================================================

    /**
     * Mutua i nuovi valori di punteggio sull'interfaccia utente assicurandosi 
     * di non fare mai rollback dei dati (es. se la REST arrive dopo un pacchetto MQTT fresco).
     */
    private void aggiornaUI(int b, int r) {
        Platform.runLater(() -> {
            int currB = Integer.parseInt(lblScoreBlue.getText());
            int currR = Integer.parseInt(lblScoreRed.getText());
            
            // Logica monotonica crescente: previene sovrascritture da latenza differenziale.
            if (b < currB || r < currR) return;

            lblScoreBlue.setText(String.valueOf(b));
            lblScoreRed.setText(String.valueOf(r));

            int maxScore = Math.max(b, r);
            double prog = (double) maxScore / SCORE_TO_WIN;
            progressWin.setProgress(Math.min(prog, 1.0));

            // Transizione dallo stato "Iniziale" allo stato "Live".
            if (!isFinished && lblStatus.getText().contains("ATTESA")) {
                lblStatus.setText("PARTITA IN CORSO");
            }

            // Calcolo e consolidamento dell'evento di fine partita (soglia di vincita).
            if (!isFinished && (b >= SCORE_TO_WIN || r >= SCORE_TO_WIN)) {
                isFinished = true;
                lblStatus.setText(b >= SCORE_TO_WIN ? "VITTORIA SQUADRA BLU!" : "VITTORIA SQUADRA ROSSA!");
                showBackButton();
            }
        });
    }

    /**
     * Rende fruibile il bottone per tornare alla schermata madre 
     * unicamente alla fine formale della gara.
     */
    private void showBackButton() {
        if (btnBackToDashboard != null) {
            btnBackToDashboard.setVisible(true);
            btnBackToDashboard.setManaged(true);
        }
    }

    /**
     * Esegue il context-switch manuale durante una partita o in caso di imprevisti.
     * Ripristina la navigazione alla vista dell'arena calciobalilla.
     *
     * @param event L'azione UI che ha invocato il ritorno.
     */
    @FXML
    void handleBack(ActionEvent event) {
        eseguiRitorno(event);
    }

    /**
     * Esegue il context-switch di schermata alla fine della partita.
     *
     * @param event L'azione UI che ha invocato il ritorno.
     */
    @FXML
    void handleBackToDashboard(ActionEvent event) {
        eseguiRitorno(event);
    }

    /**
     * Routine di utility condivisa per invalidare la sessione e commutare la vista
     * verso l'hub del calciobalilla in modo sicuro.
     * * @param event L'evento JavaFX di innesco per recuperare la finestra corrente.
     */
    private void eseguiRitorno(ActionEvent event) {
        shutdown(); 
        SessionContext.setCurrentSessionId(null);
        SessionContext.setCurrentSessionStatusUrl(null);
        
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/CalciobalillaUtenteView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1024, 768));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Funzione di teardown tecnico per arrestare sia l'animazione di timeline 
     * JavaFX sia il tunnel socket Mosquitto.
     */
    public void shutdown() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
        }
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                System.out.println("[Scoreboard] MQTT disconnesso.");
            } catch (MqttException e) {
                System.err.println("[Scoreboard] Errore disconnessione MQTT: " + e.getMessage());
            }
        }
    }
}