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
 * Motore di gioco dell'Edge: valida i sensor event e ora tiene lo STATO LIVE autoritativo di ogni
 * partita attiva (turno, punteggi, tiri a freccette) in memoria. La logica live e' migrata qui dal
 * Cloud: l'Edge decide di chi e' il turno, aggiorna il punteggio ad ogni evento (anche i MISS),
 * alterna il turno e a fine partita produce l'esito finale arricchito riportato al Cloud via MQTT.
 * Lo stato iniziale (giocatori, ordine, turno) NON e' piu' caricato via REST: il Cloud lo pubblica
 * su MQTT (topic edge-match-sync) quando la partita va IN_PROGRESS, cosi' l'Edge opera in autonomia
 * anche a Cloud irraggiungibile (nessuna chiamata sincrona sul percorso caldo).
 */
@Service
@Slf4j
public class RuleEngineService {

    private final ObjectMapper objectMapper;

    // Stato live per matchId, popolato dal push MQTT del Cloud (initFromSync).
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
     * true se l'azione di {@code userId} e' ammessa. Fail-open se lo stato live non e' presente
     * (sync non ancora ricevuto o partita non live): meglio non applicare il turno che deadlockare
     * la partita bloccando entrambi i giocatori.
     */
    public boolean isPlayersTurn(String matchId, String userId) {
        LocalMatchState s = states.get(matchId);
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
        LocalMatchState s = states.get(matchId);
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

    /**
     * Payload finale ARRICCHITO verso il Cloud: oltre ai punteggi esatti (scoreByTeam) include i
     * giocatori connessi (playerUserIds) e il vincitore (winnerName). Serializzato e pubblicato via
     * MQTT (QoS1) dall'EventForwardingService attraverso il buffer offline, cosi' l'esito di una
     * partita conclusa a Cloud irraggiungibile non va perso.
     */
    public Map<String, Object> buildResultPayload(LocalMatchState s) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("matchId", s.matchId);
        p.put("scoreByTeam", new LinkedHashMap<>(s.scoreByTeam));
        p.put("playerUserIds", new ArrayList<>(s.playerUserIds));
        p.put("winnerName", s.winnerName);
        return p;
    }

    /** Libera lo stato live di una partita conclusa (chiamato dopo aver pubblicato l'esito). */
    public void clearState(String matchId) {
        states.remove(matchId);
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

    /**
     * Inizializza (o rimpiazza) lo stato live da un push MQTT del Cloud (topic edge-match-sync).
     * Sostituisce il vecchio loadRoster REST: nessuna chiamata sincrona, l'Edge riceve lo stato
     * completo (giocatori, punteggi, turno) quando la partita va IN_PROGRESS. Ignora payload non
     * IN_PROGRESS. {@code m} e' il MatchDto serializzato dal match-service.
     */
    public void initFromSync(JsonNode m) {
        if (m == null) {
            return;
        }
        String matchId = text(m, "id");
        if (matchId == null || !"IN_PROGRESS".equals(text(m, "status"))) {
            return; // tracciamo solo partite live
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
