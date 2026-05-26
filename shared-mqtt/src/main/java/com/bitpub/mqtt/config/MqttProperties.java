package com.bitpub.mqtt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bitpub.mqtt")
public class MqttProperties {
    private String brokerUrl = "tcp://localhost:1883";
    private String clientId = "bitpub-client-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    private String username;
    private String password;
    private int connectionTimeout = 10;
    private int keepAliveInterval = 60;
    private boolean cleanSession = true;
    private boolean automaticReconnect = true;
}
