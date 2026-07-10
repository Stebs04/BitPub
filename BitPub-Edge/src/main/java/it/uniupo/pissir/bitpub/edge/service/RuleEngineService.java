/**
 * Autore: Timothy Giolito 20054431
 */
package it.uniupo.pissir.bitpub.edge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motore di gioco locale per il nodo Edge. Valida gli eventi dei sensori e gestisce in memoria
 * lo stato della partita corrente, fungendo da sorgente autoritativa per turni, punteggi e tiri.
 * Manteniamo qui tutta la logica di gioco: l'Edge determina a chi tocca, calcola i punteggi
 * per ogni azione (inclusi i mancati bersagli), gestisce l'alternanza dei giocatori e, al termine,
 * impacchetta il risultato finale per inviarlo al Cloud tramite MQTT.
 * Lo stato iniziale viene ricevuto via MQTT non appena la partita inizia, permettendoci di operare
 * senza interruzioni anche in assenza di connettività verso il server centrale.
 */
@Service
@Slf4j
public class RuleEngineService {

    private final ObjectMapper objectMapper;

    // Mappa per tenere in memoria lo stato delle partite correnti indicizzate per ID, popolate dal Cloud.
    private final Map<String, LocalMatchState> states = new ConcurrentHashMap<>();

    public RuleEngineService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public Optional<SensorEvent> validateAndParse(String payload) {
        try {
            SensorEvent event = objectMapper.readValue(payload, SensorEvent.class);
            if (event.getEventId() == null || event.getGameInstanceId() == null || event.getSensorType() == null) {
                log.warn("Invalid SensorEvent: missing required fields. Payload: {}", payload);
                return Optional.empty();
            }
            return Optional.of(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse SensorEvent from payload: {}", payload, e);
            return Optional.empty();
        }
    }

    /**
     * Struttura dati per lo stato della partita, mantenuto in memoria per tutta la durata del match sull'Edge.
     */
    public static class LocalMatchState {
        public String matchId;
        public String gameInstanceId;
        public String localeId;
        public String gameTypeId;
        public boolean darts;                 // Indica se si gioca a freccette, influenzando il cambio turno.
        public int winTarget = Integer.MAX_VALUE; // Punteggio da raggiungere per la vittoria
        public final List<String> teamOrder = new ArrayList<>();       // [teamAName, teamBName]
        public final List<String> playerUserIds = new ArrayList<>();   // [userA, userB], stesso ordine
        public final Map<String, Integer> scoreByTeam = new LinkedHashMap<>();
        public String currentTurnUserId;
        public int throwsThisTurn = 0;
        public String winnerName;
        public boolean finished = false;
    }

    // ── Gestione Turni ──────────────────────────────────────────────────────────────────────────

    /**
     * Verifica se il giocatore specificato è autorizzato ad agire in questo momento.
     * In caso di stato mancante concediamo comunque il turno per non bloccare la partita,
     * preferendo un approccio permissivo per evitare stalli.
     */
    public boolean isPlayersTurn(String matchId, String userId) {
        LocalMatchState s = states.get(matchId);
        if (s == null || s.currentTurnUserId == null) {
            return true;
        }
        return s.currentTurnUserId.equals(userId);
    }

    /**
     * Recupera lo stato attuale della partita. Il Controller lo usa per capire
     * a quale squadra appartiene il giocatore prima di passare l'azione al simulatore.
     */
    public LocalMatchState getState(String matchId) {
        return states.get(matchId);
    }

    // ── Gestione Eventi e Punteggi ──────────────────────────────────────────────────────────────

    /**
     * Aggiorna lo stato della partita applicando l'evento del sensore appena ricevuto.
     * Si occupa di assegnare i punti, avanzare i turni secondo le regole del gioco specifico
     * e dichiarare la fine del match se necessario.
     * Restituisce lo stato aggiornato, oppure vuoto se la partita non è attiva o non tracciata.
     */
    public Optional<LocalMatchState> applyEvent(SensorEvent event) {
        String matchId = event.getMatchId();
        if (matchId == null || matchId.isBlank()) {
            return Optional.empty(); // Saltiamo gli eventi non interattivi che non necessitano di uno stato.
        }
        LocalMatchState s = states.get(matchId);
        if (s == null || s.finished) {
            return Optional.empty();
        }
        return applyToState(s, event);
    }

    /**
     * Calcola il nuovo stato partendo da quello attuale e dall'evento, mantenendo la logica
     * isolata per facilitare i test senza dipendenze dal Cloud.
     */
    Optional<LocalMatchState> applyToState(LocalMatchState s, SensorEvent event) {
        Map<String, Object> p = event.getPayload();
        String team = p != null && p.get("team") != null ? p.get("team").toString() : null;
        int inc = intOf(p, "scoreIncrement", 0);
        s.winTarget = intOf(p, "winScoreTarget", s.winTarget);
        String type = event.getSensorType();

        if ("MATCH_END".equals(type)) {
            finish(s);
            return Optional.of(s);
        }
        if ("MATCH_START".equals(type)) {
            return Optional.of(s); // stato gia' inizializzato dal roster
        }

        // Aggiorniamo il punteggio sommando il valore ricevuto e verifichiamo la condizione di vittoria.
        if (inc != 0 && team != null && s.scoreByTeam.containsKey(team)) {
            int ns = s.scoreByTeam.get(team) + inc;
            s.scoreByTeam.put(team, ns);
            if (ns >= s.winTarget) {
                finish(s);
                return Optional.of(s);
            }
        }

        // Gestiamo il passaggio di turno. Per le freccette il cambio avviene ogni 3 tiri, compresi i tiri a vuoto.
        if (s.darts) {
            s.throwsThisTurn++;
            if (s.throwsThisTurn >= 3) {
                s.throwsThisTurn = 0;
                s.currentTurnUserId = other(s, s.currentTurnUserId);
            }
        } else {
            s.currentTurnUserId = other(s, s.currentTurnUserId);
        }
        return Optional.of(s);
    }

    /**
     * Costruisce i dati sullo stato della partita pronti per essere pubblicati sul broker locale per il frontend.
     */
    public Map<String, Object> buildStatePayload(LocalMatchState s, String eventMessage) {
        String a = !s.teamOrder.isEmpty() ? s.teamOrder.get(0) : "A";
        String b = s.teamOrder.size() > 1 ? s.teamOrder.get(1) : "B";
        Map<String, Object> st = new LinkedHashMap<>();
        st.put("matchId", s.matchId);
        st.put("gameTypeId", s.gameTypeId);
        st.put("status", s.finished ? "FINISHED" : "PLAYING");
        st.put("teamAName", a);
        st.put("teamBName", b);
        st.put("scoreTeamA", s.scoreByTeam.getOrDefault(a, 0));
        st.put("scoreTeamB", s.scoreByTeam.getOrDefault(b, 0));
        st.put("timeRemainingSeconds", 0);
        st.put("currentEventMessage", eventMessage);
        st.put("winnerName", s.finished ? s.winnerName : null);
        st.put("currentTurnUserId", s.currentTurnUserId);
        return st;
    }

    /**
     * Genera il riepilogo di fine partita con tutti i dettagli: punteggi, partecipanti e il nome del vincitore.
     * Questo pacchetto viene inviato al Cloud tramite il buffer offline per garantire che il risultato
     * arrivi a destinazione anche in caso di disconnessione.
     */
    public Map<String, Object> buildResultPayload(LocalMatchState s) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("matchId", s.matchId);
        p.put("scoreByTeam", new LinkedHashMap<>(s.scoreByTeam));
        p.put("playerUserIds", new ArrayList<>(s.playerUserIds));
        p.put("winnerName", s.winnerName);
        return p;
    }

    /**
     * Pulisce dalla memoria lo stato della partita una volta che l'esito è stato pubblicato con successo.
     */
    public void clearState(String matchId) {
        states.remove(matchId);
    }

    // ── Metodi di Supporto ──────────────────────────────────────────────────────────────────────

    private void finish(LocalMatchState s) {
        s.finished = true;
        s.currentTurnUserId = null;
        s.winnerName = leader(s);
    }

    /**
     * Determina la squadra in testa calcolando il punteggio maggiore. Restituisce null in caso di pareggio.
     */
    private String leader(LocalMatchState s) {
        String best = null;
        int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : s.scoreByTeam.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
            if (e.getValue() < min) { min = e.getValue(); }
        }
        return max == min ? null : best;
    }

    /**
     * Passa il turno al prossimo giocatore disponibile nella lista dei partecipanti.
     */
    private String other(LocalMatchState s, String current) {
        if (s.playerUserIds.size() < 2) return current;
        String a = s.playerUserIds.get(0), b = s.playerUserIds.get(1);
        return a.equals(current) ? b : a;
    }

    /**
     * Prepara lo stato iniziale della partita a partire dai dati ricevuti dal Cloud.
     * Elaboriamo le informazioni su squadre e turni solo se la partita è attivamente in corso,
     * ignorando i messaggi per match in altri stati.
     */
    public void initFromSync(JsonNode m) {
        if (m == null) {
            return;
        }
        String matchId = text(m, "id");
        if (matchId == null || !"IN_PROGRESS".equals(text(m, "status"))) {
            return; // Gestiamo e teniamo in memoria unicamente le partite attualmente in corso.
        }
        LocalMatchState s = new LocalMatchState();
        s.matchId = matchId;
        s.gameInstanceId = text(m, "gameInstanceId");
        s.localeId = text(m, "localeId");
        s.gameTypeId = text(m, "gameTypeId");
        String gt = s.gameTypeId != null ? s.gameTypeId.toLowerCase() : "";
        s.darts = gt.contains("freccette") || gt.contains("dart");

        for (JsonNode t : m.path("teams")) {
            String name = text(t, "name");
            if (name == null) continue;
            s.teamOrder.add(name);
            s.scoreByTeam.put(name, t.path("score").asInt(0));
            JsonNode pids = t.path("playerIds");
            if (pids.isArray() && pids.size() > 0 && !pids.get(0).isNull()) {
                s.playerUserIds.add(pids.get(0).asText());
            }
        }
        String seeded = text(m, "currentTurnUserId");
        s.currentTurnUserId = seeded != null ? seeded
                : (s.playerUserIds.isEmpty() ? null : s.playerUserIds.get(0));
        states.put(matchId, s);
        log.info("Edge initialized live state from cloud sync for match {} (darts={}, players={}, turn={})",
                matchId, s.darts, s.playerUserIds, s.currentTurnUserId);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        String s = v.asText();
        return s == null || s.isBlank() ? null : s;
    }

    private static int intOf(Map<String, Object> p, String key, int def) {
        if (p == null || p.get(key) == null) return def;
        Object v = p.get(key);
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
