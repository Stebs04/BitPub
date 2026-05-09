package com.bitpub.mqtt;

import com.bitpub.events.SessionForceStoppedEvent;
import com.bitpub.events.SessionStartedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MqttEventPublisher {

    @Autowired
    private CloudMqttGateway cloudMqttGateway;

    @EventListener
    public void onSessionForceStopped(SessionForceStoppedEvent event) {
        cloudMqttGateway.publishForceStop(event.getTableId());
    }

    @EventListener
    public void onSessionStarted(SessionStartedEvent event) {
        cloudMqttGateway.publishUnlockBalls(event.getTableId(), event.getSessionId());
    }
}
