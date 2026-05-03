package com.bitpub.mqtt;

import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.repository.GameSessionEntity;
import com.bitpub.repository.GameSessionRepository;
import com.bitpub.repository.PartitaCalciobalillaRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway di comunicazione MQTT Cloud-Side bidirezionale.
 *
 * <p>Ascolta due canali:</p>
 * <ul>
 *   <li>{@code bitpub/edge/+/score} – eventi dall'Edge Node (FoosballEvent JSON)</li>
 *   <li>{@code bitpub/locali/+/calciobalilla/+/eventi} – eventi dal modulo BitPub-Simulators
 *       (PartitaCalciobalilla JSON con campi goalRossi, goalBlu, totaleRullate, orarioFine)</li>
 * </ul>
 *
 * <p>Quando una partita termina (status FINISHED/FORCE_STOPPED su Edge, oppure
 * orarioFine != null su Simulators), il risultato viene persisto in {@code partita_calciobalilla}.</p>
 */
@Component
public class CloudMqttGateway implements MqttCallback {

    private static final String BROKER_URL = "tcp://localhost:1883";
    private final String CLIENT_ID = "BitPub-Cloud-Gateway-" + java.util.UUID.randomUUID().toString();

    private static final int MAX_GOALS = 10;

    private MqttClient client;
    private final Gson gson = new Gson();

    /** Heartbeat: timestamp dell'ultimo ping per ogni Edge Node. */
    private final Map<String, Instant> edgeLastSeen = new ConcurrentHashMap<>();

    /**
     * Tiene traccia degli ID sessione Edge già salvati in {@code partita_calciobalilla}
     * per evitare inserimenti doppi nel caso in cui arrivino due eventi FINISHED consecutivi.
     */
    private final Set<Long> savedEdgeSessions = ConcurrentHashMap.newKeySet();

    /**
     * Tiene traccia dei topic Simulators per cui la partita corrente è già stata salvata.
     * Viene resettato quando il simulatore inizia una nuova partita (score torna a 0-0).
     */
    private final Map<String, Boolean> simulatorMatchSaved = new ConcurrentHashMap<>();

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private PartitaCalciobalillaRepository partitaCalciobalillaRepository;

    public Map<String, Instant> getEdgeLastSeen() {
        return edgeLastSeen;
    }

    @PostConstruct
    public void startGateway() {
        try {
            client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            client.setCallback(this);
            client.connect(options);

            client.subscribe("bitpub/edge/heartbeat", 1);
            client.subscribe("bitpub/edge/+/score", 1);
            // Ascolto anche gli eventi del modulo BitPub-Simulators per salvarli a fine match
            client.subscribe("bitpub/locali/+/calciobalilla/+/eventi", 0);

            System.out.println("[CLOUD GATEWAY] Connesso a " + BROKER_URL
                    + " — in ascolto su edge/score, edge/heartbeat e locali/calciobalilla/eventi");
        } catch (MqttException e) {
            System.err.println("[CLOUD GATEWAY] Errore critico durante l'avvio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // messageArrived
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        System.out.println("[MQTT IN] " + topic + " | " + payload);

        if (topic.equals("bitpub/edge/heartbeat")) {
            handleHeartbeat(payload);

        } else if (topic.matches("bitpub/edge/.+/score")) {
            handleEdgeScore(payload);

        } else if (topic.matches("bitpub/locali/.+/calciobalilla/.+/eventi")) {
            handleSimulatorsEvent(topic, payload);
        }
    }

    // ── Handler: heartbeat ───────────────────────────────────────────────────

    private void handleHeartbeat(String payload) {
        try {
            JsonObject json = gson.fromJson(payload, JsonObject.class);
            if (json.has("nodeId")) {
                edgeLastSeen.put(json.get("nodeId").getAsString(), Instant.now());
            }
        } catch (Exception e) {
            System.err.println("[CLOUD GATEWAY] Errore parsing heartbeat: " + e.getMessage());
        }
    }

    // ── Handler: Edge score events ───────────────────────────────────────────

    /**
     * Processa gli eventi di punteggio pubblicati dall'Edge Node.
     * Aggiorna {@code game_session} e, a fine partita, salva in {@code partita_calciobalilla}.
     */
    private void handleEdgeScore(String payload) {
        try {
            JsonObject json = gson.fromJson(payload, JsonObject.class);

            int    scoreBlue = json.has("scoreBlue") ? json.get("scoreBlue").getAsInt() : 0;
            int    scoreRed  = json.has("scoreRed")  ? json.get("scoreRed").getAsInt()  : 0;
            String status    = json.has("status")    ? json.get("status").getAsString()  : "IN_PROGRESS";

            // ── Trova la sessione ──────────────────────────────────────────
            Optional<GameSessionEntity> sessionOpt = Optional.empty();
            Long sessionId = null;

            if (json.has("sessionId") && !json.get("sessionId").isJsonNull()) {
                sessionId = json.get("sessionId").getAsLong();
                sessionOpt = gameSessionRepository.findById(sessionId);
            } else if (json.has("tableId") && !json.get("tableId").isJsonNull()) {
                int tableId = json.get("tableId").getAsInt();
                sessionOpt = gameSessionRepository.findByTableIdAndStatus(tableId, "IN_PROGRESS");
                if (sessionOpt.isPresent()) {
                    sessionId = sessionOpt.get().getId();
                }
            }

            if (sessionOpt.isEmpty()) {
                System.err.println("[CLOUD GATEWAY] Nessuna sessione trovata per l'evento Edge score.");
                return;
            }

            GameSessionEntity session = sessionOpt.get();

            // ── Aggiorna game_session ─────────────────────────────────────
            if ("IN_PROGRESS".equals(session.getStatus())) {
                session.setScoreBlue(scoreBlue);
                session.setScoreRed(scoreRed);

                boolean isFinished = "FINISHED".equals(status) || "FORCE_STOPPED".equals(status);
                if (isFinished) {
                    session.setStatus(status);
                    session.setFinishedAt(LocalDateTime.now());
                }
                gameSessionRepository.save(session);
                System.out.println("[CLOUD GATEWAY] game_session aggiornata — Blu:" + scoreBlue
                        + " Rossi:" + scoreRed + " [" + status + "]");

                // ── Salva in partita_calciobalilla a fine partita ─────────
                if (isFinished && sessionId != null && !savedEdgeSessions.contains(sessionId)) {
                    savedEdgeSessions.add(sessionId);
                    salvaPartitaCalciobalilla(
                            scoreBlue, scoreRed,
                            scoreBlue + scoreRed,
                            0,  // totaleRullate non disponibile dal FoosballEvent
                            0,  // durataMediaPallinaSecondi non disponibile
                            session.getStartedAt(),
                            session.getFinishedAt()
                    );
                }
            }

        } catch (Exception e) {
            System.err.println("[CLOUD GATEWAY] Errore gestione edge/score: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Handler: BitPub-Simulators events ────────────────────────────────────

    /**
     * Processa gli eventi pubblicati dal modulo {@code BitPub-Simulators}
     * ({@code PartitaCalciobalilla} JSON serializzato con GSON @Expose).
     *
     * <p>La fine di una partita è riconoscibile da:<br>
     * - {@code orarioFine} presente e non-null nel payload (campo @Expose impostato
     *   solo dopo la vittoria di una squadra), oppure<br>
     * - uno dei due punteggi ha raggiunto MAX_GOALS.</p>
     */
    private void handleSimulatorsEvent(String topic, String payload) {
        try {
            JsonObject json = gson.fromJson(payload, JsonObject.class);

            int goalBlu    = json.has("goalBlu")    ? json.get("goalBlu").getAsInt()    : 0;
            int goalRossi  = json.has("goalRossi")  ? json.get("goalRossi").getAsInt()  : 0;
            int totGol     = json.has("totaleGol")  ? json.get("totaleGol").getAsInt()  : goalBlu + goalRossi;
            int rullate    = json.has("totaleRullate")           ? json.get("totaleRullate").getAsInt()           : 0;
            int durata     = json.has("durataMediaPallinaSecondi") ? json.get("durataMediaPallinaSecondi").getAsInt() : 0;

            boolean orarioFinePresente = json.has("orarioFine")
                    && !json.get("orarioFine").isJsonNull();
            boolean punteggioVittoria  = goalBlu >= MAX_GOALS || goalRossi >= MAX_GOALS;

            // Nuova partita → reset del flag "già salvata"
            if (goalBlu == 0 && goalRossi == 0) {
                simulatorMatchSaved.remove(topic);
            }

            // Fine partita → salva (una sola volta per match)
            if ((orarioFinePresente || punteggioVittoria) && !simulatorMatchSaved.getOrDefault(topic, false)) {
                simulatorMatchSaved.put(topic, true);

                salvaPartitaCalciobalilla(
                        goalBlu, goalRossi,
                        totGol,
                        rullate,
                        durata,
                        null,           // orarioInizio non disponibile in modo affidabile
                        LocalDateTime.now()
                );
            }

        } catch (Exception e) {
            System.err.println("[CLOUD GATEWAY] Errore gestione Simulators event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Persistenza ──────────────────────────────────────────────────────────

    /**
     * Crea e salva un record in {@code partita_calciobalilla}.
     *
     * @param goalBlu                   Gol della squadra blu
     * @param goalRossi                 Gol della squadra rossa
     * @param totaleGol                 Totale gol nella partita
     * @param totaleRullate             Totale rullate/falli rilevati
     * @param durataMediaPallinaSecondi Durata media pallina in secondi
     * @param orarioInizio              Timestamp di inizio (può essere null)
     * @param orarioFine                Timestamp di fine
     */
    private void salvaPartitaCalciobalilla(int goalBlu, int goalRossi,
                                           int totaleGol, int totaleRullate,
                                           int durataMediaPallinaSecondi,
                                           LocalDateTime orarioInizio,
                                           LocalDateTime orarioFine) {
        try {
            PartitaCalciobalilla partita = new PartitaCalciobalilla(
                    totaleGol, totaleRullate, durataMediaPallinaSecondi,
                    goalRossi, goalBlu
            );
            partita.setOrarioInizio(orarioInizio != null ? orarioInizio : orarioFine);
            partita.setOrarioFine(orarioFine);
            partita.setLocaleId(1L); // Locale fisso; da estendere se multi-locale

            partitaCalciobalillaRepository.save(partita);

            System.out.println("[CLOUD GATEWAY] ✅ partita_calciobalilla salvata — "
                    + "Blu:" + goalBlu + " Rossi:" + goalRossi
                    + " Gol totali:" + totaleGol
                    + " Rullate:" + totaleRullate);
        } catch (Exception e) {
            System.err.println("[CLOUD GATEWAY] Errore salvataggio partita_calciobalilla: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Comandi verso Edge ────────────────────────────────────────────────────

    /**
     * Pubblica il comando per sbloccare le palline (Start Partita).
     * Propaga il sessionId affinché l'Edge possa includerlo in ogni evento score.
     */
    public void publishUnlockBalls(Integer tableId, Long sessionId) {
        JsonObject json = new JsonObject();
        json.addProperty("tableId", tableId);
        json.addProperty("sessionId", sessionId);
        publishMessage("bitpub/cloud/foosball/start", json.toString(), 1);
    }

    /**
     * Pubblica il comando per forzare la chiusura di un tavolo (Admin Force Stop).
     */
    public void publishForceStop(Integer tableId) {
        JsonObject json = new JsonObject();
        json.addProperty("tableId", tableId);
        publishMessage("bitpub/cloud/foosball/force-stop", json.toString(), 1);
    }

    private void publishMessage(String topic, String payload, int qos) {
        if (client != null && client.isConnected()) {
            try {
                MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                msg.setQos(qos);
                client.publish(topic, msg);
                System.out.println("[MQTT OUT] " + topic + " | " + payload);
            } catch (MqttException e) {
                System.err.println("[CLOUD GATEWAY] Errore publish: " + e.getMessage());
            }
        } else {
            System.err.println("[CLOUD GATEWAY] Impossibile inviare: client MQTT non connesso.");
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[CLOUD GATEWAY] Connessione persa (il client tenterà il ripristino): " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { /* noop */ }
}
