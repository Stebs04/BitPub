package it.uniupo.pissir.bitpub.userservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.userservice.dto.CreateUserRequest;
import it.uniupo.pissir.bitpub.userservice.dto.UpdatePasswordRequest;
import it.uniupo.pissir.bitpub.userservice.dto.UpdateUserRequest;
import it.uniupo.pissir.bitpub.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Consumes Edge-forwarded user CUD commands from the Cloud system-action MQTT topic and orchestrates
 * the matching {@link UserService} call. The inner payload JSON carries an {@code action} discriminant
 * (CREATE / UPDATE_ROLE / UPDATE / UPDATE_PASSWORD / DELETE) plus the data. Replaces the former direct
 * REST calls from the WebApp to /api/v1/users.
 *
 * <p>The Edge validated the caller's JWT but not their role (MQTT carries no HTTP headers, so the
 * gateway {@code @PreAuthorize} no longer runs on this path). User management is PLATFORM_ADMIN-only,
 * so the role is re-checked here from the wrapper before any mutation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemActionCommandListener {

    private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "systemActionInboundChannel")
    public void onSystemAction(Message<String> message) {
        try {
            MqttCommandWrapper wrapper = objectMapper.readValue(message.getPayload(), MqttCommandWrapper.class);
            if (!PLATFORM_ADMIN.equals(wrapper.actorRole())) {
                log.warn("Rejected user action from non-admin actor {} (role {})", wrapper.actorUserId(), wrapper.actorRole());
                return;
            }
            JsonNode p = objectMapper.readTree(wrapper.payload());
            String action = p.path("action").asText();
            switch (action) {
                case "CREATE" -> userService.createUser(toCreate(p));
                case "UPDATE_ROLE" -> userService.updateUserRole(p.path("id").asText(), p.path("role").asText());
                case "UPDATE" -> userService.updateUserDetails(p.path("id").asText(), toUpdate(p));
                case "UPDATE_PASSWORD" -> userService.updateUserPassword(p.path("id").asText(), toPassword(p));
                case "DELETE" -> userService.deleteUser(p.path("id").asText());
                default -> log.warn("Unknown user action: {}", action);
            }
            log.info("Processed user action {} via MQTT (actor {})", action, wrapper.actorUserId());
        } catch (BitpubException e) {
            // Genuine rejection (409 duplicate, 400 bad role, 404 not found). Log, don't crash the
            // subscriber or nack the QoS1 message — a retry would only replay the same rejected command.
            log.info("Rejected user action via MQTT ({}): {}", e.getStatus(), e.getMessage());
        } catch (Exception e) {
            // Poison message — swallow so it doesn't wedge the durable queue.
            log.error("Failed to process inbound user command: {}", message.getPayload(), e);
        }
    }

    private CreateUserRequest toCreate(JsonNode p) {
        CreateUserRequest r = new CreateUserRequest();
        r.setUsername(p.path("username").asText());
        r.setPassword(p.path("password").asText());
        r.setEmail(p.path("email").asText());
        r.setRole(p.path("role").asText());
        return r;
    }

    private UpdateUserRequest toUpdate(JsonNode p) {
        UpdateUserRequest r = new UpdateUserRequest();
        r.setUsername(p.path("username").asText());
        r.setEmail(p.path("email").asText());
        return r;
    }

    private UpdatePasswordRequest toPassword(JsonNode p) {
        UpdatePasswordRequest r = new UpdatePasswordRequest();
        r.setNewPassword(p.path("newPassword").asText());
        return r;
    }
}
