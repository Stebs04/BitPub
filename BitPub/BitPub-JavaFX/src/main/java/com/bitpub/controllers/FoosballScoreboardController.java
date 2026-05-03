package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.network.SessionContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller del tabellone calciobalilla in tempo reale.
 *
 * <p>Ascolta due canali MQTT alternativi:</p>
 * <ul>
 *   <li><b>bitpub/locali/+/calciobalilla/+/eventi</b> – pubblicato dal modulo
 *       {@code BitPub-Simulators} (sempre in esecuzione); payload: {@code PartitaCalciobalilla}
 *       con campi {@code goalBlu}/{@code goalRossi}.</li>
 *   <li><b>bitpub/edge/+/score</b> – pubblicato dall'Edge Node quando il simulatore
 *       è stato avviato via comando MQTT; payload: {@code FoosballEvent}
 *       con campi {@code scoreBlue}/{@code scoreRed}/{@code status}.</li>
 * </ul>
 *
 * <p>All'apertura pubblica automaticamente il comando di start sull'Edge
 * ({@code bitpub/cloud/foosball/start}) per avviare il simulatore integrato
 * anche quando il Cloud Gateway MQTT non è raggiungibile.</p>
 */
public class FoosballScoreboardController implements MqttCallback {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label       lblScoreBlue;
    @FXML private Label       lblScoreRed;
    @FXML private Label       lblStatus;
    @FXML private ProgressBar progressMatch;
    @FXML private Button      btnBackToDashboard;

    // ── Config ────────────────────────────────────────────────────────────────
    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final int    MAX_GOALS  = 10;

    /**
     * Topic pubblicato da BitPub-Simulators (PartitaCalciobalilla JSON).
     * Formato campi: goalBlu, goalRossi, totaleGol, totaleRullate, durataMediaPallinaSecondi.
     */
    private static final String TOPIC_SIMULATORS = "bitpub/locali/+/calciobalilla/+/eventi";

    /**
     * Topic pubblicato dall'Edge Node (FoosballEvent JSON).
     * Formato campi: scoreBlue, scoreRed, status, eventType, sessionId, tableId.
     */
    private static final String TOPIC_EDGE       = "bitpub/edge/+/score";

    /** Topic su cui il Cloud (o noi come fallback) manda il comando di start all'Edge. */
    private static final String TOPIC_START_CMD  = "bitpub/cloud/foosball/start";

    // ── Stato interno ─────────────────────────────────────────────────────────
    private MqttClient mqttClient;
    private Thread     pollingThread;
    private volatile boolean running = true;
    private final Gson gson = new Gson();

    private volatile int prevScoreBlue = 0;
    private volatile int prevScoreRed  = 0;

    // ══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        if (btnBackToDashboard != null) {
            btnBackToDashboard.setVisible(false);
            btnBackToDashboard.setManaged(false);
        }
        if (progressMatch != null) {
            progressMatch.setProgress(0);
        }
        if (lblStatus != null) {
            lblStatus.setText("IN CORSO");
        }

        connectAndListen();
        startPollingFallback();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MQTT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Connette il client MQTT, si iscrive ad entrambi i canali di score
     * e pubblica il comando di start all'Edge come fallback (nel caso in cui
     * il Cloud Gateway MQTT non sia riuscito a farlo).
     */
    private void connectAndListen() {
        try {
            String clientId = "JavaFX-Scoreboard-" + System.currentTimeMillis();
            mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());

            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(true);
            opts.setAutomaticReconnect(true);
            opts.setConnectionTimeout(5);

            mqttClient.setCallback(this);
            mqttClient.connect(opts);

            // Sottoscrizione a entrambi i canali di dati di gioco
            mqttClient.subscribe(TOPIC_SIMULATORS, 0);
            mqttClient.subscribe(TOPIC_EDGE, 1);

            System.out.println("[Scoreboard] MQTT connesso — ascolto su:");
            System.out.println("  " + TOPIC_SIMULATORS + "  (BitPub-Simulators)");
            System.out.println("  " + TOPIC_EDGE        + "  (Edge Node)");

            // Pubblica il comando di start all'Edge come fallback
            // (il Cloud dovrebbe averlo già fatto, ma se MQTT era irraggiungibile non è arrivato)
            publishEdgeStartCommand();

        } catch (MqttException e) {
            System.err.println("[Scoreboard] MQTT non raggiungibile, uso solo REST polling: " + e.getMessage());
        }
    }

    /**
     * Pubblica {@code bitpub/cloud/foosball/start} con tableId=1 e il sessionId
     * dal {@link SessionContext}, così l'Edge avvia il proprio simulatore.
     */
    private void publishEdgeStartCommand() {
        if (mqttClient == null || !mqttClient.isConnected()) return;
        try {
            JsonObject cmd = new JsonObject();
            cmd.addProperty("tableId", 1);
            Long sessionId = SessionContext.getCurrentSessionId();
            if (sessionId != null) {
                cmd.addProperty("sessionId", sessionId);
            }
            MqttMessage msg = new MqttMessage(cmd.toString().getBytes(StandardCharsets.UTF_8));
            msg.setQos(1);
            mqttClient.publish(TOPIC_START_CMD, msg);
            System.out.println("[Scoreboard] Comando start inviato all'Edge: " + cmd);
        } catch (MqttException e) {
            System.err.println("[Scoreboard] Impossibile inviare start command: " + e.getMessage());
        }
    }

    // ── MqttCallback ─────────────────────────────────────────────────────────

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        System.out.println("[Scoreboard] MQTT ← [" + topic + "] " + payload);

        try {
            JsonObject json = gson.fromJson(payload, JsonObject.class);

            if (topic.matches("bitpub/locali/.+/calciobalilla/.+/eventi")) {
                // ── Formato BitPub-Simulators (PartitaCalciobalilla) ──────────
                // Campi: goalBlu, goalRossi, totaleGol, totaleRullate
                int blue = json.has("goalBlu")    ? json.get("goalBlu").getAsInt()    : prevScoreBlue;
                int red  = json.has("goalRossi")  ? json.get("goalRossi").getAsInt()  : prevScoreRed;

                // La partita termina quando uno raggiunge MAX_GOALS
                String status = (blue >= MAX_GOALS || red >= MAX_GOALS) ? "FINISHED" : "IN_PROGRESS";
                Platform.runLater(() -> applyUpdate(blue, red, status, "GOAL"));

            } else if (topic.matches("bitpub/edge/.+/score")) {
                // ── Formato Edge (FoosballEvent) ──────────────────────────────
                // Campi: scoreBlue, scoreRed, status, eventType
                int    blue   = json.has("scoreBlue") ? json.get("scoreBlue").getAsInt() : prevScoreBlue;
                int    red    = json.has("scoreRed")  ? json.get("scoreRed").getAsInt()  : prevScoreRed;
                String status = json.has("status")    ? json.get("status").getAsString()  : "IN_PROGRESS";
                String evType = json.has("eventType") ? json.get("eventType").getAsString() : "GOAL";
                Platform.runLater(() -> applyUpdate(blue, red, status, evType));
            }

        } catch (Exception e) {
            System.err.println("[Scoreboard] Errore parsing evento MQTT: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[Scoreboard] Connessione MQTT persa (fallback su REST polling): " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { /* noop */ }

    // ══════════════════════════════════════════════════════════════════════════
    // REST polling (fallback)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Interroga ogni 3 secondi l'endpoint Cloud per mantenere il DB sincronizzato
     * e come fallback quando MQTT non è disponibile.
     */
    private void startPollingFallback() {
        pollingThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(3000);
                    if (!running) break;

                    // Usa sempre il path relativo per evitare la doppia-prefix con baseUrl HATEOAS
                    String body = RestClient.getInstance().sendGet("/api/v1/sessions/foosball/current");
                    JsonObject outer = gson.fromJson(body, JsonObject.class);

                    // EntityModel HATEOAS: i dati DTO stanno direttamente al livello root
                    int    blue   = outer.has("scoreBlue") ? outer.get("scoreBlue").getAsInt() : prevScoreBlue;
                    int    red    = outer.has("scoreRed")  ? outer.get("scoreRed").getAsInt()  : prevScoreRed;
                    String status = outer.has("status")    ? outer.get("status").getAsString()  : "IN_PROGRESS";

                    Platform.runLater(() -> applyUpdate(blue, red, status, null));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // La partita potrebbe non esistere ancora → silenzioso
                    System.err.println("[Scoreboard] Poll REST fallito: " + e.getMessage());
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Aggiornamento UI
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Applica i nuovi punteggi alla UI. Deve essere chiamato sul JavaFX Application Thread.
     *
     * @param scoreBlue punteggio squadra blu
     * @param scoreRed  punteggio squadra rossa
     * @param status    stato della partita (IN_PROGRESS / FINISHED / FORCE_STOPPED)
     * @param eventType tipo evento opzionale (GOAL / START / FORCE_STOPPED)
     */
    private void applyUpdate(int scoreBlue, int scoreRed, String status, String eventType) {
        lblScoreBlue.setText(String.valueOf(scoreBlue));
        lblScoreRed.setText(String.valueOf(scoreRed));

        // Progress bar: percentuale del punteggio più alto rispetto al max
        if (progressMatch != null) {
            int maxScore = Math.max(scoreBlue, scoreRed);
            progressMatch.setProgress((double) maxScore / MAX_GOALS);
        }

        if ("FINISHED".equals(status) || "FORCE_STOPPED".equals(status)) {
            if ("FORCE_STOPPED".equals(status)) {
                lblStatus.setText("PARTITA INTERROTTA");
            } else if (scoreBlue >= MAX_GOALS) {
                lblStatus.setText("VITTORIA SQUADRA BLU! 🏆");
            } else {
                lblStatus.setText("VITTORIA SQUADRA ROSSA! 🏆");
            }
            showBackButton();
            shutdown();

        } else {
            // Mostra il messaggio contestuale solo se c'è un nuovo goal
            if (scoreBlue > prevScoreBlue) {
                lblStatus.setText("GOL SQUADRA BLU! 🔵");
                resetStatusAfterDelay();
            } else if (scoreRed > prevScoreRed) {
                lblStatus.setText("GOL SQUADRA ROSSA! 🔴");
                resetStatusAfterDelay();
            } else if (lblStatus.getText().startsWith("IN ATTESA")) {
                lblStatus.setText("IN CORSO");
            }
        }

        prevScoreBlue = scoreBlue;
        prevScoreRed  = scoreRed;
    }

    private void resetStatusAfterDelay() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    String cur = lblStatus.getText();
                    if (!cur.contains("VITTORIA") && !cur.contains("TERMINATA") && !cur.contains("INTERROTTA")) {
                        lblStatus.setText("IN CORSO");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void showBackButton() {
        if (btnBackToDashboard != null) {
            btnBackToDashboard.setVisible(true);
            btnBackToDashboard.setManaged(true);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Navigazione e Cleanup
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void handleBackToDashboard(ActionEvent event) {
        shutdown();
        SessionContext.setCurrentSessionId(null);
        SessionContext.setCurrentSessionStatusUrl(null);
        try {
            Parent root  = FXMLLoader.load(getClass().getResource("/DashboardView.fxml"));
            Stage  stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1024, 768));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Ferma polling e MQTT in modo sicuro. */
    public void shutdown() {
        running = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
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
