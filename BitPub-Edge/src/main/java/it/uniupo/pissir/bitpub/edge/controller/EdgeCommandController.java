package it.uniupo.pissir.bitpub.edge.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.security.JwtUtils;
import it.uniupo.pissir.bitpub.edge.service.EventForwardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Inbound REST ingress for WebApp commands that must survive a cloud outage: interactive
 * game actions and tournament match results. The WebApp calls the Edge (reachable on the
 * local network even when the cloud is down) instead of the gateway.
 * <p>
 * Flow: validate the caller's JWT once here (Edge as trusted origin), try the cloud
 * immediately and relay its response so online play is unchanged; if the cloud is
 * unreachable, buffer the command and return 202 — the OfflineSyncScheduler replays it later,
 * stamping the captured identity as X-User-Id/X-User-Role so it authorizes even if the JWT
 * has since expired.
 */
@RestController
@RequestMapping("/edge")
@CrossOrigin(origins = "*") // ponytail: demo-open CORS; lock to the WebApp origin for production
@Slf4j
public class EdgeCommandController {

    private final JwtUtils jwtUtils;
    private final EventForwardingService forwardingService;
    private final ObjectMapper objectMapper;

    @Value("${bitpub.cloud.match-service-url}")
    private String matchServiceUrl;

    @Value("${bitpub.cloud.tournament-service-url}")
    private String tournamentServiceUrl;

    public EdgeCommandController(JwtUtils jwtUtils, EventForwardingService forwardingService, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.forwardingService = forwardingService;
        this.objectMapper = objectMapper;
    }

    /** Interactive game action from the turn player. Body = GameActionRequestDto JSON incl. eventId. */
    @PostMapping("/matches/{matchId}/action")
    public ResponseEntity<String> gameAction(@PathVariable("matchId") String matchId,
                                             @RequestBody String actionJson,
                                             @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Actor actor = authenticate(authHeader);
        UUID eventId = extractEventId(actionJson);
        String url = matchServiceUrl + "/api/matches/" + matchId + "/action";
        return relayOrBuffer(eventId, matchId, "POST", url, actionJson, actor);
    }

    /** Tournament match result reported by a LOCALE_ADMIN. No body; params carry the outcome. */
    @PutMapping("/tournaments/{tournamentId}/matches/{tMatchId}/result")
    public ResponseEntity<String> tournamentResult(@PathVariable("tournamentId") String tournamentId,
                                                   @PathVariable("tMatchId") String tMatchId,
                                                   @RequestParam("winnerId") String winnerId,
                                                   @RequestParam(name = "stats", required = false) String stats,
                                                   @RequestParam("eventId") String eventId,
                                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Actor actor = authenticate(authHeader);
        String url = tournamentServiceUrl + "/api/v1/tournaments/" + tournamentId + "/matches/" + tMatchId
                + "/result?winnerId=" + enc(winnerId) + "&eventId=" + enc(eventId)
                + (stats != null ? "&stats=" + enc(stats) : "");
        return relayOrBuffer(UUID.fromString(eventId), tournamentId, "PUT", url, null, actor);
    }

    // ── internals ───────────────────────────────────────────────────────────────

    private record Actor(String userId, String role) {}

    private Actor authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return new Actor(jwtUtils.getUserIdFromToken(token), jwtUtils.getRoleFromToken(token));
    }

    /**
     * Try the cloud now and relay its response (online path — behaves exactly like a direct call).
     * On unreachable/5xx, buffer for deferred replay and return 202. A 4xx is a real rejection
     * (e.g. 403 not-your-turn) and is surfaced to the caller, never buffered.
     */
    private ResponseEntity<String> relayOrBuffer(UUID eventId, String contextTag, String method,
                                                 String url, String bodyJson, Actor actor) {
        try {
            RestClient.RequestBodySpec spec = RestClient.create()
                    .method(HttpMethod.valueOf(method))
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Id", actor.userId());
            if (actor.role() != null) spec = spec.header("X-User-Role", actor.role());
            String resp = (bodyJson != null && !bodyJson.isBlank())
                    ? spec.body(bodyJson).retrieve().body(String.class)
                    : spec.retrieve().body(String.class);
            log.info("Command {} relayed online to cloud: {} {} (actor {})", eventId, method, url, actor.userId());
            return ResponseEntity.ok(resp);
        } catch (ResourceAccessException io) {
            forwardingService.bufferCommand(eventId, contextTag, method, url, bodyJson, actor.userId(), actor.role());
            log.warn("Cloud unreachable, buffered command {} ({} {})", eventId, method, url);
            return ResponseEntity.accepted()
                    .body("{\"status\":\"BUFFERED_OFFLINE\",\"eventId\":\"" + eventId + "\"}");
        } catch (RestClientResponseException http) {
            if (http.getStatusCode().is5xxServerError()) {
                forwardingService.bufferCommand(eventId, contextTag, method, url, bodyJson, actor.userId(), actor.role());
                log.warn("Cloud 5xx, buffered command {} ({} {})", eventId, method, url);
                return ResponseEntity.accepted()
                        .body("{\"status\":\"BUFFERED_OFFLINE\",\"eventId\":\"" + eventId + "\"}");
            }
            // 4xx: genuine rejection (e.g. 403 not-your-turn, 409 match not in progress).
            // Surface it, do not buffer. Logged so the command is visible even when rejected.
            log.info("Command {} relayed to cloud but rejected {} ({} {}): {}",
                    eventId, http.getStatusCode(), method, url, http.getResponseBodyAsString());
            return ResponseEntity.status(http.getStatusCode()).body(http.getResponseBodyAsString());
        }
    }

    /** Reads eventId from the action body; generates one if the client omitted it (idempotency key). */
    private UUID extractEventId(String actionJson) {
        try {
            JsonNode node = objectMapper.readTree(actionJson);
            JsonNode idNode = node.get("eventId");
            if (idNode != null && !idNode.isNull() && !idNode.asText().isBlank()) {
                return UUID.fromString(idNode.asText());
            }
        } catch (Exception e) {
            log.warn("Could not parse eventId from action body, generating one", e);
        }
        return UUID.randomUUID();
    }

    private static String enc(String v) {
        return UriUtils.encode(v, StandardCharsets.UTF_8);
    }
}
