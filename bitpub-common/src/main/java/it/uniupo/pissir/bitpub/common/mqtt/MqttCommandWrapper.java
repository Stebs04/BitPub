package it.uniupo.pissir.bitpub.common.mqtt;

/**
 * Envelope for a WebApp command relayed Edge -> Cloud over MQTT. MQTT carries no HTTP headers,
 * so the caller identity (validated by the Edge's JWT check) is packed into the message instead
 * of X-User-Id / X-User-Role headers.
 *
 * @param actorUserId authenticated caller id (from the Edge-validated JWT)
 * @param actorRole   caller role, may be null
 * @param targetId    id the command acts on (matchId / tournamentId), also encoded in the topic
 * @param payload     the original request body as raw JSON (e.g. GameActionRequestDto)
 */
public record MqttCommandWrapper(String actorUserId, String actorRole, String targetId, String payload) {}
