package it.uniupo.pissir.bitpub.edge.service;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.edge.service.RuleEngineService.LocalMatchState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RuleEngineServiceTest {

    private final RuleEngineService ruleEngineService = new RuleEngineService("http://localhost:8085");

    // ── validateAndParse ────────────────────────────────────────────────────────

    @Test
    void validateAndParse_ValidPayload_ReturnsEvent() {
        String id = UUID.randomUUID().toString();
        String payload = "{\"eventId\":\"" + id + "\",\"gameInstanceId\":\"game1\",\"sensorType\":\"INFRARED\",\"timestamp\":\"2026-06-29T10:00:00Z\"}";

        Optional<SensorEvent> result = ruleEngineService.validateAndParse(payload);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getEventId().toString());
        assertEquals("game1", result.get().getGameInstanceId());
        assertEquals("INFRARED", result.get().getSensorType());
    }

    @Test
    void validateAndParse_MissingRequiredFields_ReturnsEmpty() {
        String payload = "{\"sensorType\":\"INFRARED\"}"; // missing gameInstanceId
        assertFalse(ruleEngineService.validateAndParse(payload).isPresent());
    }

    @Test
    void validateAndParse_InvalidJson_ReturnsEmpty() {
        assertFalse(ruleEngineService.validateAndParse("{ invalid_json }").isPresent());
    }

    // ── logica live migrata dal Cloud: turno, 3-tiri freccette, vittoria ─────────

    private LocalMatchState state(boolean darts) {
        LocalMatchState s = new LocalMatchState();
        s.gameTypeId = darts ? "freccette" : "calciobalilla";
        s.darts = darts;
        s.teamOrder.add("A");
        s.teamOrder.add("B");
        s.playerUserIds.add("userA");
        s.playerUserIds.add("userB");
        s.scoreByTeam.put("A", 0);
        s.scoreByTeam.put("B", 0);
        s.currentTurnUserId = "userA";
        return s;
    }

    private SensorEvent ev(String type, String team, int inc, int target) {
        Map<String, Object> p = new HashMap<>();
        p.put("team", team);
        p.put("scoreIncrement", inc);
        p.put("winScoreTarget", target);
        return SensorEvent.builder().eventId(UUID.randomUUID()).gameInstanceId("gi").matchId("m")
                .sensorType(type).timestamp(Instant.now()).payload(p).build();
    }

    @Test
    void nonDarts_scoresAndAlternatesEveryThrow_missIncluded() {
        LocalMatchState s = state(false);

        ruleEngineService.applyToState(s, ev("GOAL", "A", 5, 100));
        assertEquals(5, s.scoreByTeam.get("A"));
        assertEquals("userB", s.currentTurnUserId, "il turno passa all'avversario ad ogni tiro");

        // MISS (scoreIncrement 0): niente punti ma il turno avanza comunque.
        ruleEngineService.applyToState(s, ev("MISS", "B", 0, 100));
        assertEquals(0, s.scoreByTeam.get("B"));
        assertEquals("userA", s.currentTurnUserId);
    }

    @Test
    void darts_alternatesOnlyAfterThreeThrows() {
        LocalMatchState s = state(true);

        ruleEngineService.applyToState(s, ev("DART_HIT", "A", 20, 501));
        assertEquals("userA", s.currentTurnUserId, "tiro 1/3: stesso giocatore");
        ruleEngineService.applyToState(s, ev("MISS", "A", 0, 501));
        assertEquals("userA", s.currentTurnUserId, "tiro 2/3: stesso giocatore (anche su MISS)");
        ruleEngineService.applyToState(s, ev("DART_HIT", "A", 20, 501));
        assertEquals("userB", s.currentTurnUserId, "tiro 3/3: turno all'avversario");
        assertEquals(0, s.throwsThisTurn);
    }

    @Test
    void reachingTarget_finishesAndSetsWinner_turnCleared() {
        LocalMatchState s = state(false);

        ruleEngineService.applyToState(s, ev("GOAL", "A", 10, 10));
        assertTrue(s.finished);
        assertEquals("A", s.winnerName);
        assertNull(s.currentTurnUserId, "a partita finita non c'e' turno");
    }

    @Test
    void drawHasNoWinner() {
        LocalMatchState s = state(false);
        s.scoreByTeam.put("A", 9);
        s.scoreByTeam.put("B", 10);
        ruleEngineService.applyToState(s, ev("GOAL", "A", 1, 10)); // A raggiunge 10 -> pari con B
        assertTrue(s.finished);
        assertNull(s.winnerName, "pareggio: nessun vincitore");
    }
}
