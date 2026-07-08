package it.uniupo.pissir.bitpub.common.constants;

public final class MqttTopics {
    
    private MqttTopics() {}
    
    // bitpub/sensors/{localeId}/{gameInstanceId}/event
    public static final String SENSOR_EVENT_FORMAT = "bitpub/sensors/%s/%s/event";
    
    // bitpub/actuators/{localeId}/{gameInstanceId}/command
    public static final String ACTUATOR_COMMAND_FORMAT = "bitpub/actuators/%s/%s/command";
    
    // bitpub/edge/{localeId}/heartbeat
    public static final String EDGE_HEARTBEAT_FORMAT = "bitpub/edge/%s/heartbeat";
    
    // bitpub/edge/{localeId}/sync/request
    public static final String EDGE_SYNC_REQUEST_FORMAT = "bitpub/edge/%s/sync/request";
    
    // bitpub/edge/{localeId}/sync/ack
    public static final String EDGE_SYNC_ACK_FORMAT = "bitpub/edge/%s/sync/ack";
    
    // bitpub/platform/notifications/{userId}
    public static final String NOTIFICATIONS_FORMAT = "bitpub/platform/notifications/%s";
    
    // bitpub/match/{localeId}/{gameInstanceId}/state
    public static final String GAME_STATE_FORMAT = "bitpub/match/%s/%s/state";

    // bitpub/cloud/sensors/{gameInstanceId}/event — Edge -> Cloud forwarded (validated) sensor events.
    // Distinct from the raw local bitpub/sensors/# topic so match-service consumes only Edge-validated
    // events (no bypass) and Edge does not re-consume its own republish (no loop).
    public static final String CLOUD_SENSOR_INGEST_FORMAT = "bitpub/cloud/sensors/%s/event";

    // bitpub/cloud/commands/matches/{matchId}/action — Edge -> Cloud interactive game actions.
    // MQTT has no HTTP headers, so the caller identity travels inside the MqttCommandWrapper payload.
    public static final String CLOUD_MATCH_ACTION_FORMAT = "bitpub/cloud/commands/matches/%s/action";

    public static String getSensorEventTopic(String localeId, String gameInstanceId) {
        return String.format(SENSOR_EVENT_FORMAT, localeId, gameInstanceId);
    }
    
    public static String getActuatorCommandTopic(String localeId, String gameInstanceId) {
        return String.format(ACTUATOR_COMMAND_FORMAT, localeId, gameInstanceId);
    }
    
    public static String getEdgeHeartbeatTopic(String localeId) {
        return String.format(EDGE_HEARTBEAT_FORMAT, localeId);
    }
    
    public static String getGameStateTopic(String localeId, String gameInstanceId) {
        return String.format(GAME_STATE_FORMAT, localeId, gameInstanceId);
    }

    public static String getCloudSensorIngestTopic(String gameInstanceId) {
        return String.format(CLOUD_SENSOR_INGEST_FORMAT, gameInstanceId);
    }

    public static String getCloudMatchActionTopic(String matchId) {
        return String.format(CLOUD_MATCH_ACTION_FORMAT, matchId);
    }
}
