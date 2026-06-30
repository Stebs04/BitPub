package it.uniupo.pissir.bitpub.edge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
public class EdgeMqttIntegrationTest {

    @Container
    public static GenericContainer<?> mosquittoContainer = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2"))
            .withExposedPorts(1883);

    @DynamicPropertySource
    static void mosquittoProperties(DynamicPropertyRegistry registry) {
        registry.add("mqtt.broker.url", () -> "tcp://" + mosquittoContainer.getHost() + ":" + mosquittoContainer.getMappedPort(1883));
        registry.add("mqtt.client.id", () -> "bitpub-edge-test");
    }

    @Test
    void testMosquittoContainerIsRunning() {
        assertTrue(mosquittoContainer.isRunning());
    }

    // Qui andrebbero implementati i test specifici di invio/ricezione messaggi MQTT
    // testando l'integrazione di Spring Integration MQTT.
}
