package it.uniupo.pissir.bitpub.tournamentservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.service.impl.TournamentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Consumes Edge-forwarded tournament CUD commands from the Cloud system-action MQTT topic and
 * orchestrates the matching {@link TournamentServiceImpl} call. The inner payload JSON carries an
 * {@code action} discriminant (CREATE_TOURNAMENT / UPDATE_TOURNAMENT / DELETE_TOURNAMENT) plus the
 * data. Replaces the former direct REST calls from the WebApp to /api/v1/tournaments.
 *
 * <p>Tournaments are LOCALE_ADMIN-only and locale-scoped: every ownership check needs the admin's
 * localeId. The gateway used to inject it as {@code X-User-Locale-Id}, but the MqttCommandWrapper
 * carries only actorUserId + actorRole (MQTT has no HTTP headers). So the role is re-checked here and
 * the localeId is resolved from the actor via locale-service — the same lookup statistics-service
 * uses. ponytail: one REST hop per command keeps this contained to tournament-service; the leaner
 * alternative is to add localeId to MqttCommandWrapper and have the Edge fill it from the JWT claim.
 */
@Component
@Slf4j
public class SystemActionCommandListener {

    private static final String LOCALE_ADMIN = "LOCALE_ADMIN";

    private final TournamentServiceImpl tournamentService;
    private final ObjectMapper objectMapper;
    private final String localeServiceUrl;

    public SystemActionCommandListener(TournamentServiceImpl tournamentService,
                                       ObjectMapper objectMapper,
                                       @Value("${locale.service.url:http://localhost:8083}") String localeServiceUrl) {
        this.tournamentService = tournamentService;
        this.objectMapper = objectMapper;
        this.localeServiceUrl = localeServiceUrl;
    }

    @ServiceActivator(inputChannel = "systemActionInboundChannel")
    public void onSystemAction(Message<String> message) {
        try {
            MqttCommandWrapper wrapper = objectMapper.readValue(message.getPayload(), MqttCommandWrapper.class);
            if (!LOCALE_ADMIN.equals(wrapper.actorRole())) {
                log.warn("Rejected tournament action from non-admin actor {} (role {})", wrapper.actorUserId(), wrapper.actorRole());
                return;
            }
            String localeId = resolveAdminLocaleId(wrapper.actorUserId());
            if (localeId == null) {
                log.warn("Rejected tournament action: locale of actor {} not determinable", wrapper.actorUserId());
                return;
            }

            JsonNode p = objectMapper.readTree(wrapper.payload());
            String action = p.path("action").asText();
            switch (action) {
                case "CREATE_TOURNAMENT" -> {
                    TournamentDto dto = toTournament(p);
                    dto.setLocaleIds(List.of(localeId)); // il locale del torneo e' sempre quello dell'admin
                    tournamentService.createTournament(dto);
                }
                case "UPDATE_TOURNAMENT" -> {
                    String id = p.path("id").asText();
                    assertOwns(id, localeId);
                    TournamentDto dto = toTournament(p);
                    dto.setLocaleIds(List.of(localeId)); // il locale resta il proprio
                    tournamentService.updateTournament(id, dto);
                }
                case "DELETE_TOURNAMENT" -> {
                    String id = p.path("id").asText();
                    assertOwns(id, localeId);
                    tournamentService.deleteTournament(id);
                }
                default -> log.warn("Unknown tournament action: {}", action);
            }
            log.info("Processed tournament action {} via MQTT (actor {})", action, wrapper.actorUserId());
        } catch (BitpubException e) {
            // Genuine rejection (403 not owner, 404 not found, 409 has registrations). Log, don't crash
            // the subscriber or nack the QoS1 message — a retry would only replay the same rejection.
            log.info("Rejected tournament action via MQTT ({}): {}", e.getStatus(), e.getMessage());
        } catch (Exception e) {
            // Poison message — swallow so it doesn't wedge the durable queue.
            log.error("Failed to process inbound tournament command: {}", message.getPayload(), e);
        }
    }

    /** Il torneo deve appartenere al locale del chiamante (replica del controllo del controller REST). */
    private void assertOwns(String id, String localeId) {
        List<String> localeIds = tournamentService.getTournament(id).getLocaleIds();
        if (localeIds == null || !localeIds.contains(localeId)) {
            throw new BitpubException("Il torneo non appartiene al tuo locale", HttpStatus.FORBIDDEN);
        }
    }

    /** localeId del locale gestito dall'adminId, o null se nessuno (stesso lookup di statistics-service). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private String resolveAdminLocaleId(String adminId) {
        if (adminId == null) return null;
        try {
            List response = RestClient.create(localeServiceUrl)
                    .get()
                    .uri("/api/v1/locales/by-admin/{adminId}", adminId)
                    .retrieve()
                    .body(List.class);
            if (response != null && !response.isEmpty() && response.get(0) instanceof Map) {
                Object localeId = ((Map) response.get(0)).get("id");
                return localeId != null ? localeId.toString() : null;
            }
        } catch (Exception e) {
            log.error("Failed to resolve locale for adminId {}", adminId, e);
        }
        return null;
    }

    private TournamentDto toTournament(JsonNode p) {
        return TournamentDto.builder()
                .name(p.path("name").asText())
                .gameTypeId(p.path("gameTypeId").asText())
                .teamBased(p.path("teamBased").asBoolean())
                .maxParticipants(p.hasNonNull("maxParticipants") ? p.path("maxParticipants").asInt() : null)
                .build();
    }
}
