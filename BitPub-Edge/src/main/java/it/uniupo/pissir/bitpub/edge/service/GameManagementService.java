package it.uniupo.pissir.bitpub.edge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the Edge node's local view of installed game instances in sync with the cloud catalog.
 * locale-service publishes ADD/REMOVE events on bitpub/locales/games/{localeId}; this service
 * applies them to an in-memory registry (gameInstanceId -> gameTypeId).
 */
@Service
@Slf4j
public class GameManagementService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ponytail: in-memory registry, ephemeral by design; persist to edge-db if state must survive restart.
    private final Map<String, String> installedGames = new ConcurrentHashMap<>();

    @ServiceActivator(inputChannel = "cloudGamesInputChannel")
    public void handleGameEvent(Message<String> message) {
        String payload = message.getPayload();
        try {
            JsonNode node = objectMapper.readTree(payload);
            String action = node.path("action").asText();
            String gameInstanceId = node.path("gameInstanceId").asText();
            String gameTypeId = node.path("gameTypeId").asText();

            if ("ADD".equals(action)) {
                installedGames.put(gameInstanceId, gameTypeId);
                log.info("Game ADDED to local state: instance={} type={}", gameInstanceId, gameTypeId);
            } else if ("REMOVE".equals(action)) {
                installedGames.remove(gameInstanceId);
                log.info("Game REMOVED from local state: instance={}", gameInstanceId);
            } else {
                log.warn("Unknown game event action: {}", action);
            }
        } catch (Exception e) {
            log.error("Failed to process game event: {}", payload, e);
        }
    }
}
