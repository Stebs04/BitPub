package com.bitpub.mqtt.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * MqttTopicRegistry centralizes the topic naming strategy and topic versioning.
 * <p>
 * Current Version: v1
 * Structure: {version}/{domain}/{id}/{action_or_status}
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MqttTopicRegistry {

    private static final String VERSION = "v1";

    // System topics
    private static final String SYSTEM_PREFIX = VERSION + "/system";
    
    // Game topics
    private static final String GAMES_PREFIX = VERSION + "/games";
    
    // Edge topics
    private static final String EDGE_PREFIX = VERSION + "/edge";
    
    // Stats topics
    private static final String STATS_PREFIX = VERSION + "/stats";
    
    // DLT and Retry topics
    public static final String DLT_PREFIX = "dlt/";
    public static final String RETRY_PREFIX = "retry/";

    /**
     * Topic: v1/system/{service}/status
     */
    public static String systemStatus(String serviceName) {
        return String.format("%s/%s/status", SYSTEM_PREFIX, serviceName);
    }

    /**
     * Topic: v1/games/{localeId}/{gameId}/events
     */
    public static String gameEvents(String localeId, String gameId) {
        return String.format("%s/%s/%s/events", GAMES_PREFIX, localeId, gameId);
    }

    /**
     * Topic: v1/edge/{edgeId}/heartbeat
     */
    public static String edgeHeartbeat(String edgeId) {
        return String.format("%s/%s/heartbeat", EDGE_PREFIX, edgeId);
    }

    /**
     * Topic: v1/stats/global
     */
    public static String globalStats() {
        return String.format("%s/global", STATS_PREFIX);
    }

    /**
     * Generates a Dead Letter Topic for a given original topic.
     * Example: dlt/v1/games/it/123/events
     */
    public static String dlt(String originalTopic) {
        return DLT_PREFIX + originalTopic;
    }

    /**
     * Generates a Retry Topic for a given original topic.
     */
    public static String retry(String originalTopic) {
        return RETRY_PREFIX + originalTopic;
    }
}
