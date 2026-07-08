package it.uniupo.pissir.bitpub.matchservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.BitpubException;
import it.uniupo.pissir.bitpub.common.mqtt.MqttCommandWrapper;
import it.uniupo.pissir.bitpub.matchservice.dto.GameActionRequestDto;
import it.uniupo.pissir.bitpub.matchservice.service.impl.MatchServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Consumes Edge-forwarded interactive game actions from the Cloud command MQTT topic and feeds them
 * into {@link MatchServiceImpl#processGameAction} (which validates turn ownership and republishes the
 * new state to the WebApp match-state topic). Replaces the former REST POST /api/matches/{id}/action.
 * The caller identity travels in the wrapper because MQTT has no HTTP headers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommandIngestListener {

    private final MatchServiceImpl matchService;
    private final ObjectMapper objectMapper;

    @ServiceActivator(inputChannel = "mqttCommandInboundChannel")
    public void onGameAction(Message<String> message) {
        try {
            MqttCommandWrapper wrapper = objectMapper.readValue(message.getPayload(), MqttCommandWrapper.class);
            GameActionRequestDto action = objectMapper.readValue(wrapper.payload(), GameActionRequestDto.class);
            matchService.processGameAction(wrapper.targetId(), wrapper.actorUserId(), action);
            log.info("Processed game action via MQTT: match={}, actor={}", wrapper.targetId(), wrapper.actorUserId());
        } catch (BitpubException e) {
            // Genuine rejection (403 not-your-turn, 409 not in progress). Log, don't crash the subscriber
            // or nack the QoS1 message — retrying would only replay the same rejected action.
            log.info("Rejected game action via MQTT ({}): {}", e.getStatus(), e.getMessage());
        } catch (Exception e) {
            // Poison message — swallow so it doesn't wedge the queue; idempotency covers replays.
            log.error("Failed to process inbound game command: {}", message.getPayload(), e);
        }
    }
}
