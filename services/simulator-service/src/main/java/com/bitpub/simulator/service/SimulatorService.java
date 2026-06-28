package com.bitpub.simulator.service;

import com.bitpub.contracts.events.MatchEndedEvent;
import com.bitpub.contracts.events.MatchStartedEvent;
import com.bitpub.contracts.events.ScoreEvent;
import com.bitpub.mqtt.publisher.MqttPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.bitpub.mqtt.registry.MqttTopicRegistry;

@Service
public class SimulatorService {

    private final MqttPublisher mqttPublisher;
    private final Map<String, SimulatedMatch> activeSessions = new ConcurrentHashMap<>();

    public SimulatorService(MqttPublisher mqttPublisher) {
        this.mqttPublisher = mqttPublisher;
    }

    public String simulateCalciobalilla() {
        return startSimulation("TABLE_FOOTBALL");
    }

    public String simulateBiliardo() {
        return startSimulation("POOL");
    }

    public String simulateFreccette() {
        return startSimulation("DARTS");
    }

    private String startSimulation(String gameType) {
        String sessionId = UUID.randomUUID().toString();
        String deviceId = "sim-device-" + ThreadLocalRandom.current().nextInt(1000);
        activeSessions.put(sessionId, new SimulatedMatch(sessionId, gameType, deviceId));
        
        MatchStartedEvent event = MatchStartedEvent.builder()
                .eventId(UUID.randomUUID())
                .correlationId(UUID.fromString(sessionId))
                .timestamp(Instant.now())
                .source(deviceId)
                .gameId(gameType)
                .localeId("LOCALE_TEST_1")
                .matchMode("1v1")
                .players(List.of("Player1", "Player2"))
                .build();
                
        String topic = MqttTopicRegistry.gameEvents("LOCALE_TEST_1", gameType);
        mqttPublisher.publish(topic, event);
        
        return sessionId;
    }

    @Scheduled(fixedRate = 5000)
    public void incrementScoresTask() {
        for (SimulatedMatch match : activeSessions.values()) {
            if ("ONGOING".equals(match.getStatus())) {
                if (ThreadLocalRandom.current().nextInt(100) < 30) {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        match.setScore1(match.getScore1() + 1);
                    } else {
                        match.setScore2(match.getScore2() + 1);
                    }
                    
                    String topic = MqttTopicRegistry.gameEvents("LOCALE_TEST_1", match.getGameType());
                    
                    ScoreEvent scoreEvent = ScoreEvent.builder()
                            .eventId(UUID.randomUUID())
                            .correlationId(UUID.fromString(match.getSessionId()))
                            .timestamp(Instant.now())
                            .source(match.getDeviceId())
                            .gameId(match.getGameType())
                            .localeId("LOCALE_TEST_1")
                            .scoreTeamA(match.getScore1())
                            .scoreTeamB(match.getScore2())
                            .scoringTeam("TEAM_" + (ThreadLocalRandom.current().nextBoolean() ? "A" : "B"))
                            .build();
                    
                    mqttPublisher.publish(topic, scoreEvent);
                    
                    if (match.getScore1() >= 5 || match.getScore2() >= 5) {
                        match.setStatus("COMPLETED");
                        
                        MatchEndedEvent endEvent = MatchEndedEvent.builder()
                            .eventId(UUID.randomUUID())
                            .correlationId(UUID.fromString(match.getSessionId()))
                            .timestamp(Instant.now())
                            .source(match.getDeviceId())
                            .gameId(match.getGameType())
                            .localeId("LOCALE_TEST_1")
                            .winningTeam(match.getScore1() > match.getScore2() ? "TEAM_A" : "TEAM_B")
                            .finalScoreTeamA(match.getScore1())
                            .finalScoreTeamB(match.getScore2())
                            .matchDuration("300")
                            .build();
                        
                        mqttPublisher.publish(topic, endEvent);
                        activeSessions.remove(match.getSessionId());
                    }
                }
            }
        }
    }

    public static class SimulatedMatch {
        private String sessionId;
        private String gameType;
        private String deviceId;
        private int score1 = 0;
        private int score2 = 0;
        private String status = "ONGOING";

        public SimulatedMatch(String sessionId, String gameType, String deviceId) {
            this.sessionId = sessionId;
            this.gameType = gameType;
            this.deviceId = deviceId;
        }

        public String getSessionId() { return sessionId; }
        public String getGameType() { return gameType; }
        public String getDeviceId() { return deviceId; }
        public int getScore1() { return score1; }
        public void setScore1(int score1) { this.score1 = score1; }
        public int getScore2() { return score2; }
        public void setScore2(int score2) { this.score2 = score2; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
