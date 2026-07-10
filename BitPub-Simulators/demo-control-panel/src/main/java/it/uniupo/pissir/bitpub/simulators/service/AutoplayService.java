/**
 * autore Timothy Giolito 20054431
 *
 * Servizio che implementa la logica di gioco automatico (autoplay).
 * Quando attivato, il servizio simula iterativamente e in modo casuale l'attività
 * dei sensori di punteggio per le partite in corso. Gestisce inoltre il riavvio
 * autonomo delle partite a intervalli prestabiliti.
 */
package it.uniupo.pissir.bitpub.simulators.service;

import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.simulators.service.GenericSimulator.GameConfig;
import it.uniupo.pissir.bitpub.simulators.service.GenericSimulator.SensorRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class AutoplayService {

    private static final int RESTART_EVERY_TICKS = 6; // ~30s a 5s/tick: avvia una nuova partita

    private final GenericSimulator simulator;
    private final Map<String, AutoplayState> activeAutoplays = new ConcurrentHashMap<>();

    public AutoplayService(GenericSimulator simulator) {
        this.simulator = simulator;
    }

    public void toggleAutoplay(String gameTypeId, String localeId, String gameInstanceId, boolean enabled) {
        if (enabled) {
            activeAutoplays.put(gameInstanceId, new AutoplayState(gameTypeId, localeId, gameInstanceId));
            emitMatchStart(gameTypeId, localeId, gameInstanceId);
        } else {
            if (activeAutoplays.remove(gameInstanceId) != null) {
                emit(gameInstanceId, localeId, "MATCH_END", new HashMap<>());
            }
        }
    }

    public boolean isAutoplayEnabled(String gameInstanceId) {
        return activeAutoplays.containsKey(gameInstanceId);
    }

    @Scheduled(fixedRate = 5000)
    public void runAutoplayTick() {
        activeAutoplays.values().forEach(this::tick);
    }

    private void tick(AutoplayState state) {
        // Riavvia periodicamente: se la partita precedente e' finita, MATCH_START ne crea una nuova
        // (ignorato dal match-service se ce n'e' gia' una in corso).
        if (++state.tickCount % RESTART_EVERY_TICKS == 0) {
            emitMatchStart(state.gameTypeId, state.localeId, state.gameInstanceId);
        }

        GameConfig config = simulator.config(state.gameTypeId).orElse(null);
        if (config == null) {
            log.debug("Autoplay: no cached config for {} yet", state.gameTypeId);
            return;
        }
        List<SensorRule> scoring = config.scoringSensors();
        if (scoring.isEmpty()) return;

        SensorRule sensor = scoring.get(ThreadLocalRandom.current().nextInt(scoring.size()));
        String team = ThreadLocalRandom.current().nextBoolean() ? "Auto-Red" : "Auto-Blue";
        boolean hit = ThreadLocalRandom.current().nextDouble() < sensor.successProbability;

        Map<String, Object> payload = new HashMap<>();
        payload.put("team", team);
        payload.put("winScoreTarget", config.winScoreTarget);
        payload.put("scoreIncrement", hit ? sensor.scoreIncrement : 0);
        emit(state.gameInstanceId, state.localeId, hit ? sensor.type : "MISS", payload);
    }

    private void emitMatchStart(String gameTypeId, String localeId, String gameInstanceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("teamAName", "Auto-Red");
        payload.put("teamBName", "Auto-Blue");
        emit(gameInstanceId, localeId, "MATCH_START", payload);
    }

    private void emit(String gameInstanceId, String localeId, String type, Map<String, Object> payload) {
        SensorEvent event = SensorEvent.builder()
                .eventId(UUID.randomUUID())
                .gameInstanceId(gameInstanceId)
                .sensorType(type)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
        simulator.publish(event, localeId, gameInstanceId);
    }

    private static class AutoplayState {
        final String gameTypeId;
        final String localeId;
        final String gameInstanceId;
        int tickCount = 0;

        AutoplayState(String gameTypeId, String localeId, String gameInstanceId) {
            this.gameTypeId = gameTypeId;
            this.localeId = localeId;
            this.gameInstanceId = gameInstanceId;
        }
    }
}
