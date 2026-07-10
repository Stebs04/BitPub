/**
 * Autore: Timothy Giolito 20054431
 */
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
 * Mantiene sincronizzata la vista locale delle istanze di gioco con il catalogo del Cloud.
 * Ascolta gli eventi ADD/REMOVE e aggiorna di conseguenza un registro in memoria che mappa
 * gli id delle istanze ai tipi di gioco.
 */
@Service
@Slf4j
public class GameManagementService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Il registro in memoria è effimero. Nel caso in cui lo stato dovesse sopravvivere a un riavvio andrà salvato su db.
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
