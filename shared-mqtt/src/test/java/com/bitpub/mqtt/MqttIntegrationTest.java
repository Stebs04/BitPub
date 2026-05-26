package com.bitpub.mqtt;

import com.bitpub.mqtt.payload.SystemStatusPayload;
import com.bitpub.mqtt.publisher.MqttPublisher;
import com.bitpub.mqtt.registry.MqttTopicRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(classes = TestApplication.class)
@Testcontainers
public class MqttIntegrationTest {

    @Container
    public static GenericContainer<?> mosquittoContainer = new GenericContainer<>("eclipse-mosquitto:2.0")
            .withExposedPorts(1883)
            .withCommand("mosquitto", "-c", "/mosquitto-no-auth.conf")
            // We mount a simple config to allow anonymous for testing
            .withEnv("MOSQUITTO_ALLOW_ANONYMOUS", "true");

    @DynamicPropertySource
    static void mqttProperties(DynamicPropertyRegistry registry) {
        String brokerUrl = String.format("tcp://%s:%d",
                mosquittoContainer.getHost(),
                mosquittoContainer.getMappedPort(1883));
        registry.add("bitpub.mqtt.broker-url", () -> brokerUrl);
        registry.add("bitpub.mqtt.clean-session", () -> "true");
    }

    @Autowired
    private MqttPublisher mqttPublisher;

    @Test
    void shouldPublishMessageSuccessfully() {
        SystemStatusPayload payload = SystemStatusPayload.builder()
                .serviceName("test-service")
                .status("UP")
                .timestamp(Instant.now())
                .version("1.0.0")
                .build();

        String topic = MqttTopicRegistry.systemStatus("test-service");

        // Verify that publishing does not throw exceptions
        assertDoesNotThrow(() -> mqttPublisher.publish(topic, payload));
    }
}
