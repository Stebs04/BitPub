/**
 * autore Timothy Giolito 20054431
 *
 * Cuore del sistema di simulazione, questo servizio mantiene un registro locale
 * delle configurazioni di gioco ricevute e risponde agli eventi interattivi in entrata.
 * Al momento della ricezione di un'azione, ne determina l'esito basandosi sulle 
 * probabilità configurate e propaga i relativi eventi di successo o fallimento.
 */
package it.uniupo.pissir.bitpub.simulators.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import it.uniupo.pissir.bitpub.common.events.SensorEvent;
import it.uniupo.pissir.bitpub.simulators.config.MqttConfig.MqttPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class GenericSimulator {

    private final MqttPublisher mqttPublisher;
    private final ObjectMapper objectMapper;

    // gameTypeId -> regole del gioco. Aggiornata dai messaggi di config retained.
    private final Map<String, GameConfig> configByGameType = new ConcurrentHashMap<>();

    public GenericSimulator(MqttPublisher mqttPublisher, ObjectMapper objectMapper) {
        this.mqttPublisher = mqttPublisher;
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void onInbound(Message<String> message) {
        String topic = String.valueOf(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
        try {
            if (topic != null && topic.startsWith("bitpub/config/games/")) {
                cacheConfig(message.getPayload());
            } else if (topic != null && topic.endsWith("/action")) {
                resolveAction(message.getPayload());
            }
        } catch (Exception e) {
            log.error("GenericSimulator failed on topic {}: {}", topic, message.getPayload(), e);
        }
    }

    private void cacheConfig(String json) throws Exception {
        GameConfig config = objectMapper.readValue(json, GameConfig.class);
        if (config.id != null) {
            configByGameType.put(config.id, config);
            log.info("Cached game config {} (winScoreTarget={}, {} sensors)",
                    config.id, config.winScoreTarget, config.sensors != null ? config.sensors.size() : 0);
        }
    }

    /**
     * Risolve un'azione: RNG sulla successProbability del sensore. A segno emette il sensor event con
     * lo scoreIncrement; altrimenti un MISS (0 punti). In entrambi i casi propaga team, winScoreTarget
     * ed eventId cosi' che il match-service accrediti il punteggio al team giusto e chiuda al traguardo.
     */
    private void resolveAction(String json) throws Exception {
        Map<?, ?> action = objectMapper.readValue(json, Map.class);
        String gameTypeId = str(action.get("gameTypeId"));
        String sensorType = str(action.get("sensorType"));
        String team = str(action.get("team"));
        String localeId = str(action.get("localeId"));
        String gameInstanceId = str(action.get("gameInstanceId"));
        String matchId = str(action.get("matchId"));
        String eventId = str(action.get("eventId"));

        GameConfig config = configByGameType.get(gameTypeId);
        if (config == null && gameTypeId != null) {
            // gameTypeId a volte porta il name testuale ("foosball") invece dell'UUID: fallback per name
            config = configByGameType.values().stream()
                    .filter(c -> gameTypeId.equalsIgnoreCase(c.name))
                    .findFirst().orElse(null);
        }
        if (config == null) {
            log.warn("No cached config for gameTypeId {} — cannot resolve action {}", gameTypeId, sensorType);
            return;
        }
        SensorRule sensor = config.findSensor(sensorType);
        if (sensor == null) {
            log.warn("Sensor {} not found in config {} — ignoring action", sensorType, gameTypeId);
            return;
        }

        boolean hit = ThreadLocalRandom.current().nextDouble() < sensor.successProbability;

        Map<String, Object> payload = new HashMap<>();
        payload.put("team", team);
        payload.put("winScoreTarget", config.winScoreTarget);
        payload.put("scoreIncrement", hit ? sensor.scoreIncrement : 0);

        SensorEvent event = SensorEvent.builder()
                .eventId(eventId != null && !"null".equals(eventId) ? UUID.fromString(eventId) : UUID.randomUUID())
                .gameInstanceId(gameInstanceId)
                .matchId(matchId)
                .sensorType(hit ? sensorType : "MISS")
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        publish(event, localeId, gameInstanceId);
        log.info("Action {} on {} -> {} (team {}, +{})", sensorType, gameInstanceId,
                hit ? sensorType : "MISS", team, hit ? sensor.scoreIncrement : 0);
    }

    /** Pubblica un sensor event sul topic locale dei sensori; l'Edge lo valida e lo inoltra al cloud. */
    public void publish(SensorEvent event, String localeId, String gameInstanceId) {
        try {
            String topic = MqttTopics.getSensorEventTopic(localeId, gameInstanceId);
            mqttPublisher.sendToMqtt(objectMapper.writeValueAsString(event), topic);
        } catch (Exception e) {
            log.error("Failed to publish sensor event", e);
        }
    }

    // ── Accesso alle regole in cache (usato dall'autoplay) ─────────────────────────

    public Optional<GameConfig> config(String gameTypeId) {
        return Optional.ofNullable(configByGameType.get(gameTypeId));
    }

    /** Tutti i tipi di gioco attualmente in cache (dal catalogo via MQTT), per il pannello demo. */
    public java.util.Collection<GameConfig> configs() {
        return configByGameType.values();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    // ── Modelli di config (mappano il JSON pubblicato da game-catalog-service) ─────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameConfig {
        public String id;
        public String name;
        public int winScoreTarget;
        public List<SensorRule> sensors;

        public SensorRule findSensor(String type) {
            if (sensors == null || type == null) return null;
            return sensors.stream().filter(s -> type.equalsIgnoreCase(s.type)).findFirst().orElse(null);
        }

        /** Sensori giocabili: eventi di punteggio (non attuatori, non i controlli MATCH_START/END). */
        public List<SensorRule> scoringSensors() {
            return sensors == null ? List.of() : sensors.stream()
                    .filter(s -> !s.actuator)
                    .filter(s -> !"MATCH_START".equalsIgnoreCase(s.type) && !"MATCH_END".equalsIgnoreCase(s.type))
                    .toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SensorRule {
        public String type;
        public boolean actuator; // Jackson serializza isActuator come "actuator"
        public int scoreIncrement;
        public double successProbability;
    }
}
