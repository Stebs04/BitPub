package it.uniupo.pissir.bitpub.simulators.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AutoplayService {

    // Win conditions
    private static final int FOOSBALL_WIN_SCORE  = 10; // first to 10 goals wins
    private static final int DARTS_WIN_SCORE     = 301; // first team to reach 301+ points wins
    private static final int BILLIARDS_TOTAL_BALLS = 15; // 8-ball pocketed after all others

    @Value("${server.port:8080}")
    private String serverPort;

    // Map: gameInstanceId -> AutoplayState
    private final Map<String, AutoplayState> activeAutoplays = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();

    public void toggleAutoplay(String gameType, String localeId, String gameInstanceId, boolean enabled) {
        if (enabled) {
            activeAutoplays.put(gameInstanceId, new AutoplayState(gameType, localeId, gameInstanceId));
            // Start the first match with recognizable autoplay team names
            triggerRestEvent(gameType, localeId, gameInstanceId, "MATCH_START",
                    Map.of("teamAName", "Auto-Red", "teamBName", "Auto-Blue"));
        } else {
            AutoplayState state = activeAutoplays.remove(gameInstanceId);
            if (state != null) {
                // End the current match cleanly
                triggerRestEvent(gameType, localeId, gameInstanceId, "MATCH_END", Map.of());
            }
        }
    }

    public boolean isAutoplayEnabled(String gameInstanceId) {
        return activeAutoplays.containsKey(gameInstanceId);
    }

    @Scheduled(fixedRate = 5000)
    public void runAutoplayTick() {
        activeAutoplays.values().forEach(state -> {
            switch (state.gameType.toLowerCase()) {
                case "calciobalilla":
                    simulateFoosballTick(state);
                    break;
                case "freccette":
                    simulateDartTick(state);
                    break;
                case "biliardo":
                    simulateBilliardsTick(state);
                    break;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Foosball: first team to reach FOOSBALL_WIN_SCORE goals wins
    // -------------------------------------------------------------------------
    private void simulateFoosballTick(AutoplayState state) {
        String team = Math.random() > 0.5 ? "RED" : "BLUE";
        triggerRestEvent(state.gameType, state.localeId, state.gameInstanceId, "GOAL", Map.of("team", team));

        // Update in-memory score
        if ("RED".equals(team)) {
            state.scoreA.incrementAndGet();
        } else {
            state.scoreB.incrementAndGet();
        }

        // Check win condition
        if (state.scoreA.get() >= FOOSBALL_WIN_SCORE || state.scoreB.get() >= FOOSBALL_WIN_SCORE) {
            endAndRestartMatch(state);
        }
    }

    // -------------------------------------------------------------------------
    // Darts: first team (active player) to reach DARTS_WIN_SCORE points wins
    // -------------------------------------------------------------------------
    private void simulateDartTick(AutoplayState state) {
        int score = (int) (Math.random() * 20) + 1;
        int multiplier = Math.random() > 0.8 ? 2 : (Math.random() > 0.9 ? 3 : 1);
        triggerRestEvent(state.gameType, state.localeId, state.gameInstanceId, "DART_HIT",
                Map.of("score", score, "multiplier", multiplier));

        // Update in-memory score for team A (the active player in darts)
        state.scoreA.addAndGet(score * multiplier);

        // Check win condition
        if (state.scoreA.get() >= DARTS_WIN_SCORE) {
            endAndRestartMatch(state);
        }
    }

    // -------------------------------------------------------------------------
    // Billiards (8-ball): match ends when the 8-ball is pocketed (ball #8).
    // We cycle through balls 1-7 and 9-15, then pocket the 8-ball last.
    // -------------------------------------------------------------------------
    private void simulateBilliardsTick(AutoplayState state) {
        int pocketId   = (int) (Math.random() * 6) + 1;
        int ballNumber = state.counter.incrementAndGet();

        if (ballNumber > BILLIARDS_TOTAL_BALLS) {
            // All balls pocketed — should not reach here normally, reset
            state.counter.set(1);
            ballNumber = 1;
        }

        String color = ballNumber <= 8 ? "SOLID" : "STRIPED";
        triggerRestEvent(state.gameType, state.localeId, state.gameInstanceId, "BALL_POCKETED",
                Map.of("pocketId", pocketId, "ballNumber", ballNumber, "ballColor", color));

        state.scoreA.incrementAndGet();

        // The 8-ball (black ball) being pocketed ends the match
        if (ballNumber == BILLIARDS_TOTAL_BALLS) {
            endAndRestartMatch(state);
        }
    }

    // -------------------------------------------------------------------------
    // Helper: sends MATCH_END, resets state, then sends MATCH_START for next round
    // -------------------------------------------------------------------------
    private void endAndRestartMatch(AutoplayState state) {
        System.out.printf("[Autoplay] Match ended for %s (scoreA=%d, scoreB=%d). Restarting...%n",
                state.gameInstanceId, state.scoreA.get(), state.scoreB.get());

        triggerRestEvent(state.gameType, state.localeId, state.gameInstanceId, "MATCH_END", Map.of());

        // Reset score counters for the new match
        state.scoreA.set(0);
        state.scoreB.set(0);
        state.counter.set(0);

        triggerRestEvent(state.gameType, state.localeId, state.gameInstanceId, "MATCH_START",
                Map.of("teamAName", "Auto-Red", "teamBName", "Auto-Blue"));
    }

    private void triggerRestEvent(String gameType, String localeId, String gameInstanceId, String eventType, Map<String, Object> payload) {
        String url = String.format("http://localhost:%s/api/simulators/%s/%s/%s/event?eventType=%s&matchId=autoplay-match",
                serverPort, gameType, localeId, gameInstanceId, eventType);
        try {
            restTemplate.postForEntity(url, payload, String.class);
        } catch (Exception e) {
            System.err.println("Autoplay error: " + e.getMessage());
        }
    }

    private static class AutoplayState {
        String gameType;
        String localeId;
        String gameInstanceId;
        AtomicInteger counter = new AtomicInteger(0); // balls pocketed (billiards)
        AtomicInteger scoreA  = new AtomicInteger(0); // RED / Player A score
        AtomicInteger scoreB  = new AtomicInteger(0); // BLUE / Player B score

        public AutoplayState(String gameType, String localeId, String gameInstanceId) {
            this.gameType       = gameType;
            this.localeId       = localeId;
            this.gameInstanceId = gameInstanceId;
        }
    }
}
