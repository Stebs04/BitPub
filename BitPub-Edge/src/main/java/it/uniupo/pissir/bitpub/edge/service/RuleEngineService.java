package it.uniupo.pissir.bitpub.edge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motore di gioco dell'Edge: valida i sensor event e ora tiene lo STATO LIVE autoritativo di ogni
 * partita attiva (turno, punteggi, tiri a freccette) in memoria. La logica live e' migrata qui dal
 * Cloud: l'Edge decide di chi e' il turno, aggiorna il punteggio ad ogni evento (anche i MISS),
 * alterna il turno e a fine partita riporta l'esito finale al Cloud (POST /result) per la persistenza.
 * Il roster iniziale (giocatori, ordine, turno) viene caricato una volta da match-service via REST.
 */
@Service
@Slf4j
public class RuleEngineService {

    private final ObjectMapper objectMapper;
    private final RestClient matchClient;

    // Stato live per matchId. getOrLoad NON memorizza i null (roster non caricabile),
    // quindi un load fallito viene ritentato alla prossima azione con header valido.
    private final Map<String, LocalMatchState> states = new ConcurrentHashMap<>();

    public RuleEngineService(@Value("${bitpub.cloud.match-service-url}") String matchServiceUrl) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.matchClient = RestClient.create(matchServiceUrl);
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

    /** Stato live in-memory di una partita attiva su questo Edge. Struct interno: campi pubblici. */
    public static class LocalMatchState {
        public String matchId;
        public String gameInstanceId;
        public String localeId;
        public String gameTypeId;
        public boolean darts;                 // freccette: 3 tiri per giocatore prima di alternare
        public int winTarget = Integer.MAX_VALUE; // dal payload winScoreTarget (data-driven)
        public final List<String> teamOrder = new ArrayList<>();       // [teamAName, teamBName]
        public final List<String> playerUserIds = new ArrayList<>();   // [userA, userB], stesso ordine
        public final Map<String, Integer> scoreByTeam = new LinkedHashMap<>();
        public String currentTurnUserId;
        public int throwsThisTurn = 0;
        public String winnerName;
        public boolean finished = false;
    }

    // ── Turno: interrogato dall'EdgeCommandController prima di inoltrare un'azione ──────────────

    /**
     * true se l'azione di {@code userId} e' ammessa. Fail-open se il roster non e' caricabile
     * (Cloud irraggiungibile o partita non live): meglio non applicare il turno che deadlockare
     * la partita bloccando entrambi i giocatori.
     */
    public boolean isPlayersTurn(String matchId, String userId, String authHeader) {
        LocalMatchState s = getOrLoad(matchId, authHeader);
        if (s == null || s.currentTurnUserId == null) {
            return true;
        }
        return s.currentTurnUserId.equals(userId);
    }

    // ── Evento: aggiorna punteggio + turno, segnala fine partita ───────────────────────────────

    /**
     * Applica un sensor event allo stato live: accredita il punteggio (0 = MISS), alterna il turno
     * (freccette: dopo 3 tiri dello stesso giocatore) e marca la fine al raggiungimento del target
     * o su MATCH_END. Ritorna lo stato aggiornato, o empty se l'evento non e' legato a una partita
     * live tracciabile (matchId assente / non caricabile / gia' finita).
     */
    public Optional<LocalMatchState> applyEvent(SensorEvent event) {
        String matchId = event.getMatchId();
        if (matchId == null || matchId.isBlank()) {
            return Optional.empty(); // eventi non interattivi (es. autoplay senza matchId): nessuno stato live
        }
        LocalMatchState s = getOrLoad(matchId, null);
        if (s == null || s.finished) {
            return Optional.empty();
        }
        return applyToState(s, event);
    }

    /** Transizione pura (turno/punteggio/fine) su uno stato gia' risolto — testabile senza il Cloud. */
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

        // Punteggio: modello data-driven, somma verso winTarget (come faceva il Cloud).
        if (inc != 0 && team != null && s.scoreByTeam.containsKey(team)) {
            int ns = s.scoreByTeam.get(team) + inc;
            s.scoreByTeam.put(team, ns);
            if (ns >= s.winTarget) {
                finish(s);
                return Optional.of(s);
            }
        }

        // Turno: ogni tiro conta (anche MISS). Freccette: alterna dopo 3 tiri dello stesso giocatore.
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

    /** Payload di stato per il broker locale: stessi campi del GameState del frontend + currentTurnUserId. */
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

    /** A fine partita: riporta i punteggi finali al Cloud (persistenza + stats) e libera lo stato. */
    public void reportResultToCloud(LocalMatchState s) {
        Map<String, Integer> scores = new LinkedHashMap<>(s.scoreByTeam);
        try {
            matchClient.post()
                    .uri("/api/matches/{id}/result", s.matchId)
                    .header("Content-Type", "application/json")
                    .body(scores)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Edge reported final result for match {} to cloud: {}", s.matchId, scores);
        } catch (Exception e) {
            log.error("Edge failed to report final result for match {} to cloud", s.matchId, e);
        }
        states.remove(s.matchId);
    }

    // ── internals ──────────────────────────────────────────────────────────────────────────────

    private void finish(LocalMatchState s) {
        s.finished = true;
        s.currentTurnUserId = null;
        s.winnerName = leader(s);
    }

    /** Team col punteggio piu' alto; null se pareggio (nessun vincitore), come nel Cloud. */
    private String leader(LocalMatchState s) {
        String best = null;
        int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : s.scoreByTeam.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
            if (e.getValue() < min) { min = e.getValue(); }
        }
        return max == min ? null : best;
    }

    /** Alterna tra i due userId del roster; se non c'e' un secondo giocatore resta invariato. */
    private String other(LocalMatchState s, String current) {
        if (s.playerUserIds.size() < 2) return current;
        String a = s.playerUserIds.get(0), b = s.playerUserIds.get(1);
        return a.equals(current) ? b : a;
    }

    private LocalMatchState getOrLoad(String matchId, String authHeader) {
        LocalMatchState s = states.get(matchId);
        if (s == null && authHeader != null) {
            s = loadRoster(matchId, authHeader);
            if (s != null) {
                states.put(matchId, s);
            }
        }
        return s;
    }

    /** Carica una volta il roster della partita live dal Cloud e ne inizializza lo stato locale. */
    private LocalMatchState loadRoster(String matchId, String authHeader) {
        try {
            JsonNode m = matchClient.get().uri("/api/matches/{id}", matchId)
                    .header("Authorization", authHeader)
                    .retrieve().body(JsonNode.class);
            if (m == null || !"IN_PROGRESS".equals(text(m, "status"))) {
                return null; // tracciamo solo partite live
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
            log.info("Edge loaded live state for match {} (darts={}, players={}, turn={})",
                    matchId, s.darts, s.playerUserIds, s.currentTurnUserId);
            return s;
        } catch (Exception e) {
            log.warn("Edge could not load roster for match {} from cloud ({}); turn gate open, live state skipped",
                    matchId, e.getMessage());
            return null;
        }
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
