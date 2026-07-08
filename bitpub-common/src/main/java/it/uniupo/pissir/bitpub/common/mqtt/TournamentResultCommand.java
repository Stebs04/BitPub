package it.uniupo.pissir.bitpub.common.mqtt;

/**
 * Payload of a tournament bracket result relayed Edge -> Cloud over MQTT, carried as the raw-JSON
 * {@link MqttCommandWrapper#payload()}. The tournamentId travels in the wrapper's targetId and the
 * topic; matchId identifies the specific bracket slot to decide.
 *
 * @param matchId  bracket match to record the result for
 * @param winnerId participant id of the winner
 * @param stats    optional score/stats text, may be null
 */
public record TournamentResultCommand(String matchId, String winnerId, String stats) {}
