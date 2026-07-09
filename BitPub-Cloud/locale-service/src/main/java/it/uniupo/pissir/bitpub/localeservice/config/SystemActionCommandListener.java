package it.uniupo.pissir.bitpub.localeservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.localeservice.dto.AddGameInstanceRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.CreateLocaleRequest;
import it.uniupo.pissir.bitpub.localeservice.service.LocaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Consumes Edge-forwarded locale CUD commands from the Cloud system-action MQTT topic and orchestrates
 * the matching {@link LocaleService} call. The inner payload JSON carries an {@code action} discriminant
 * (CREATE_LOCALE / DELETE_LOCALE / ADD_GAME_INSTANCE / TOGGLE_GAME_INSTANCE / DELETE_GAME_INSTANCE) plus the data.
 *
 * <p>The Edge validated the caller's JWT but not their role. Create/delete of a locale are
 * PLATFORM_ADMIN-only, re-checked here. Add/toggle of a game instance are already authorized inside
 * LocaleService (assertLocaleManageable) against the caller id/role carried in the wrapper.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemActionCommandListener {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private final LocaleService localeService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "systemActionInboundChannel")
    public void onSystemAction(Message<String> message) {
        try {
            MqttCommandWrapper wrapper = objectMapper.readValue(message.getPayload(), MqttCommandWrapper.class);
            JsonNode p = objectMapper.readTree(wrapper.payload());
            String action = p.path("action").asText();
            String actorId = wrapper.actorUserId();
            String actorRole = wrapper.actorRole();
            switch (action) {
                case "CREATE_LOCALE" -> {
                    requirePlatformAdmin(actorRole);
                    localeService.createLocale(toCreateLocale(p));
                }
                case "DELETE_LOCALE" -> {
                    requirePlatformAdmin(actorRole);
                    localeService.deleteLocale(p.path("id").asText());
                }
                case "ADD_GAME_INSTANCE" ->
                        localeService.addGameInstance(p.path("localeId").asText(), toAddGame(p), actorId, actorRole);
                case "TOGGLE_GAME_INSTANCE" ->
                        localeService.setGameInstanceActive(p.path("localeId").asText(),
                                p.path("gameInstanceId").asText(), p.path("active").asBoolean(), actorId, actorRole);
                case "DELETE_GAME_INSTANCE" ->
                        localeService.removeGameInstance(p.path("localeId").asText(),
                                p.path("gameInstanceId").asText(), actorId, actorRole);
                default -> log.warn("Unknown locale action: {}", action);
            }
            log.info("Processed locale action {} via MQTT (actor {})", action, actorId);
        } catch (BitpubException e) {
            // Genuine rejection (403 not owner / not admin, 404 not found, 409 duplicate). Log, don't
            // crash the subscriber or nack the QoS1 message — a retry would only replay the rejection.
            log.info("Rejected locale action via MQTT ({}): {}", e.getStatus(), e.getMessage());
        } catch (Exception e) {
            // Poison message — swallow so it doesn't wedge the durable queue.
            log.error("Failed to process inbound locale command: {}", message.getPayload(), e);
        }
    }

    private void requirePlatformAdmin(String actorRole) {
        if (!PLATFORM_ADMIN.equals(actorRole)) {
            throw new BitpubException("Only PLATFORM_ADMIN can create or delete a locale", HttpStatus.FORBIDDEN);
        }
    }

    private CreateLocaleRequest toCreateLocale(JsonNode p) {
        CreateLocaleRequest r = new CreateLocaleRequest();
        r.setName(p.path("name").asText());
        r.setAddress(p.path("address").asText());
        r.setAdminId(p.path("adminId").asText());
        return r;
    }

    private AddGameInstanceRequest toAddGame(JsonNode p) {
        AddGameInstanceRequest r = new AddGameInstanceRequest();
        r.setLocalInstanceId(p.path("localInstanceId").asText());
        r.setGameTypeId(p.path("gameTypeId").asText());
        return r;
    }
}
