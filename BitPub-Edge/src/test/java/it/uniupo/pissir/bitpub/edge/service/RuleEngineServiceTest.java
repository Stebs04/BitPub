/**
 * Autore: Timothy Giolito 20054431
 */
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

    private final RuleEngineService ruleEngineService = new RuleEngineService();

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
        String payload = "{\"sensorType\":\"INFRARED\"}"; // Manca l'identificativo della partita
        assertFalse(ruleEngineService.validateAndParse(payload).isPresent());
    }

    @Test
    void validateAndParse_InvalidJson_ReturnsEmpty() {
        assertFalse(ruleEngineService.validateAndParse("{ invalid_json }").isPresent());
    }

    // ── Test sulla logica locale: turni, freccette e condizioni di vittoria ───────

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
        assertEquals("userB", s.currentTurnUserId, "Il turno deve passare all'avversario dopo un tiro nel calciobalilla");

        // Anche se si fa un tiro a vuoto, non si ottengono punti ma si passa comunque il turno.
        ruleEngineService.applyToState(s, ev("MISS", "B", 0, 100));
        assertEquals(0, s.scoreByTeam.get("B"));
        assertEquals("userA", s.currentTurnUserId);
    }

    @Test
    void darts_alternatesOnlyAfterThreeThrows() {
        LocalMatchState s = state(true);

        ruleEngineService.applyToState(s, ev("DART_HIT", "A", 20, 501));
        assertEquals("userA", s.currentTurnUserId, "Tiro 1 di 3: il turno rimane allo stesso giocatore");
        ruleEngineService.applyToState(s, ev("MISS", "A", 0, 501));
        assertEquals("userA", s.currentTurnUserId, "Tiro 2 di 3: il turno rimane allo stesso giocatore anche se sbaglia");
        ruleEngineService.applyToState(s, ev("DART_HIT", "A", 20, 501));
        assertEquals("userB", s.currentTurnUserId, "Tiro 3 di 3: il giocatore ha finito i tiri, passo al prossimo");
        assertEquals(0, s.throwsThisTurn);
    }

    @Test
    void reachingTarget_finishesAndSetsWinner_turnCleared() {
        LocalMatchState s = state(false);

        ruleEngineService.applyToState(s, ev("GOAL", "A", 10, 10));
        assertTrue(s.finished);
        assertEquals("A", s.winnerName);
        assertNull(s.currentTurnUserId, "Una volta conclusa la partita non ha più senso avere un turno attivo");
    }

    @Test
    void drawHasNoWinner() {
        LocalMatchState s = state(false);
        s.scoreByTeam.put("A", 9);
        s.scoreByTeam.put("B", 10);
        ruleEngineService.applyToState(s, ev("GOAL", "A", 1, 10)); // Segnando questo punto, la squadra A pareggia
        assertTrue(s.finished);
        assertNull(s.winnerName, "In caso di parità, non deve essere dichiarato alcun vincitore");
    }
}
