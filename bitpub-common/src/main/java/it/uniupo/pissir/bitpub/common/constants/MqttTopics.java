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

    // bitpub/cloud/commands/tournaments/{tournamentId}/matches/{matchId}/result — Edge -> Cloud
    // tournament bracket result reported by a LOCALE_ADMIN. Identity travels in the MqttCommandWrapper.
    public static final String CLOUD_TOURNAMENT_RESULT_FORMAT = "bitpub/cloud/commands/tournaments/%s/matches/%s/result";

    // bitpub/cloud/commands/system/{entity}/action — Edge -> Cloud generic CUD command for any entity
    // (users, locales, catalog, ...). Identity + body travel in the MqttCommandWrapper (MQTT has no headers).
    public static final String CLOUD_SYSTEM_ACTION_FORMAT = "bitpub/cloud/commands/system/%s/action";

    // bitpub/tournaments/{tournamentId}/state — Cloud -> WebApp live tournament (bracket) updates.
    public static final String TOURNAMENT_STATE_FORMAT = "bitpub/tournaments/%s/state";

    // bitpub/tournaments/{tournamentId}/ended — Cloud -> services/edges: tournament concluded (COMPLETED).
    public static final String TOURNAMENT_ENDED_FORMAT = "bitpub/tournaments/%s/ended";

    // bitpub/statistics/update/{gameTypeId} — Cloud -> WebApp live leaderboard updates, one topic
    // per game so the WebApp reconstructs each gametype section independently (no shared topic).
    public static final String STATISTICS_UPDATE_FORMAT = "bitpub/statistics/update/%s";

    // bitpub/cloud/matches/result — match-service -> statistics-service completed-match results (single shared topic).
    public static final String CLOUD_MATCH_RESULT_TOPIC = "bitpub/cloud/matches/result";

    // bitpub/config/games/{gameTypeId} — game-catalog-service -> simulators: full GameType+Sensors config
    // (retained) so a simulator caches the win target / score increments / RNG probabilities in memory.
    public static final String GAME_CONFIG_FORMAT = "bitpub/config/games/%s";

    // bitpub/simulators/{gameInstanceId}/action — match-service -> GenericSimulator: resolve an action's
    // RNG. The simulator rolls successProbability and emits the resulting sensor event (hit) or MISS.
    public static final String SIMULATOR_ACTION_FORMAT = "bitpub/simulators/%s/action";

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

    public static String getCloudSystemActionTopic(String entity) {
        return String.format(CLOUD_SYSTEM_ACTION_FORMAT, entity);
    }

    public static String getCloudTournamentResultTopic(String tournamentId, String matchId) {
        return String.format(CLOUD_TOURNAMENT_RESULT_FORMAT, tournamentId, matchId);
    }

    public static String getTournamentStateTopic(String tournamentId) {
        return String.format(TOURNAMENT_STATE_FORMAT, tournamentId);
    }

    public static String getTournamentEndedTopic(String tournamentId) {
        return String.format(TOURNAMENT_ENDED_FORMAT, tournamentId);
    }

    public static String getGameConfigTopic(String gameTypeId) {
        return String.format(GAME_CONFIG_FORMAT, gameTypeId);
    }

    public static String getSimulatorActionTopic(String gameInstanceId) {
        return String.format(SIMULATOR_ACTION_FORMAT, gameInstanceId);
    }

    public static String getStatisticsUpdateTopic(String gameTypeId) {
        return String.format(STATISTICS_UPDATE_FORMAT, gameTypeId);
    }
}
