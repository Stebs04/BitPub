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

    @Value("${server.port:8080}")
    private String serverPort;

    // Map: gameInstanceId -> AutoplayState
    private final Map<String, AutoplayState> activeAutoplays = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();

    public void toggleAutoplay(String gameType, String localeId, String gameInstanceId, boolean enabled) {
        if (enabled) {
            activeAutoplays.put(gameInstanceId, new AutoplayState(gameType, localeId, gameInstanceId));
            // Start match
            triggerRestEvent(gameType, localeId, gameInstanceId, "MATCH_START", Map.of());
        } else {
            AutoplayState state = activeAutoplays.remove(gameInstanceId);
            if (state != null) {
                // End match
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

    private void simulateFoosballTick(AutoplayState state) {
        String team = Math.random() > 0.5 ? "RED" : "BLUE";
        triggerRestEvent(state.gameType, state.localeId, state.gameInstanceId, "GOAL", Map.of("team", team));
    }

    private void simulateDartTick(AutoplayState state) {
        int score = (int) (Math.random() * 20) + 1;
        int multiplier = Math.random() > 0.8 ? 2 : (Math.random() > 0.9 ? 3 : 1);
        triggerRestEvent(state.gameType, state.localeId, state.gameInstanceId, "DART_HIT", Map.of("score", score, "multiplier", multiplier));
    }

    private void simulateBilliardsTick(AutoplayState state) {
        int pocketId = (int) (Math.random() * 6) + 1;
        int ballNumber = state.counter.incrementAndGet();
        if (ballNumber > 15) {
            ballNumber = 1;
            state.counter.set(1);
        }
        triggerRestEvent(state.gameType, state.localeId, state.gameInstanceId, "BALL_POCKETED", Map.of("pocketId", pocketId, "ballNumber", ballNumber, "ballColor", "STRIPED"));
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
        AtomicInteger counter = new AtomicInteger(0);

        public AutoplayState(String gameType, String localeId, String gameInstanceId) {
            this.gameType = gameType;
            this.localeId = localeId;
            this.gameInstanceId = gameInstanceId;
        }
    }
}
