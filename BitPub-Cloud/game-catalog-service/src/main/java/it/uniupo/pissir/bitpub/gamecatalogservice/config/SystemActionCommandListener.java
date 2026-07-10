/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.AddSensorRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.CreateGameTypeRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.service.GameCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Componente in ascolto sui canali MQTT dedicati alle operazioni di sistema (creazione/modifica/eliminazione) sul catalogo giochi.
 * Interpreta il payload JSON e orchestra la chiamata corrispondente sul layer di servizio.
 * Questa architettura sostituisce le vecchie interazioni dirette REST dal gateway.
 *
 * Provvede autonomamente alla verifica delle prerogative dell'attore (limitato a GAME_ADMIN),
 * sopperendo alla mancanza del contesto HTTP @PreAuthorize su chiamate veicolate tramite protocollo MQTT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemActionCommandListener {

    private static final String GAME_ADMIN = "GAME_ADMIN";

    private final GameCatalogService gameCatalogService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "systemActionInboundChannel")
    public void onSystemAction(Message<String> message) {
        try {
            MqttCommandWrapper wrapper = objectMapper.readValue(message.getPayload(), MqttCommandWrapper.class);
            if (!GAME_ADMIN.equals(wrapper.actorRole())) {
                log.warn("Rejected catalog action from non-admin actor {} (role {})", wrapper.actorUserId(), wrapper.actorRole());
                return;
            }
            JsonNode p = objectMapper.readTree(wrapper.payload());
            String action = p.path("action").asText();
            switch (action) {
                case "CREATE_GAME_TYPE" -> gameCatalogService.createGameType(toCreateGameType(p));
                case "UPDATE_GAME_TYPE" -> gameCatalogService.updateGameType(p.path("id").asText(), toCreateGameType(p));
                case "DELETE_GAME_TYPE" -> gameCatalogService.deleteGameType(p.path("id").asText());
                case "ADD_SENSOR" -> gameCatalogService.addSensorToGameType(p.path("gameTypeId").asText(), toAddSensor(p));
                case "DELETE_SENSOR" -> gameCatalogService.deleteSensor(p.path("gameTypeId").asText(), p.path("sensorId").asText());
                default -> log.warn("Unknown catalog action: {}", action);
            }
            log.info("Processed catalog action {} via MQTT (actor {})", action, wrapper.actorUserId());
        } catch (BitpubException e) {
            // Eccezione applicativa gestita (es. conflitti di nome o risorse inesistenti).
            // Si evita l'arresto del subscriber o il reject del messaggio QoS1 per impedire loop di retry infiniti.
            log.info("Rejected catalog action via MQTT ({}): {}", e.getStatus(), e.getMessage());
        } catch (Exception e) {
            // Errore critico in decodifica: il messaggio viene scartato per prevenire il blocco della coda persistente.
            log.error("Failed to process inbound catalog command: {}", message.getPayload(), e);
        }
    }

    private CreateGameTypeRequest toCreateGameType(JsonNode p) {
        CreateGameTypeRequest r = new CreateGameTypeRequest();
        r.setName(p.path("name").asText());
        r.setDescription(p.path("description").asText());
        r.setWinScoreTarget(p.path("winScoreTarget").asInt(10));
        return r;
    }

    private AddSensorRequest toAddSensor(JsonNode p) {
        AddSensorRequest r = new AddSensorRequest();
        r.setType(p.path("type").asText());
        r.setDescription(p.path("description").asText());
        r.setActuator(p.path("actuator").asBoolean());
        r.setScoreIncrement(p.path("scoreIncrement").asInt(1));
        r.setSuccessProbability(p.path("successProbability").asDouble(1.0));
        return r;
    }
}
