/**
 * Autore: Timothy Giolito 20054431
 */
package it.uniupo.pissir.bitpub.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.constants.MqttTopics;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test di integrazione per verificare il flusso MQTT tra Sensore, Edge e Cloud usando Testcontainers.
 * Controlliamo che un evento valido arrivato sul topic locale venga elaborato dal motore regole
 * e inoltrato correttamente sul topic di destinazione per il Cloud.
 * Sfruttiamo un client MQTT di supporto per accertarci che il messaggio passi lungo tutta la catena.
 */
@SpringBootTest
@Testcontainers
public class EdgeMqttIntegrationTest {

    @Container
    public static GenericContainer<?> mosquittoContainer =
            new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2"))
                    .withExposedPorts(1883)
                    // Per evitare blocchi di default di eclipse-mosquitto:2, passiamo un file di configurazione per autorizzare le connessioni anonime.
                    .withClasspathResourceMapping("mosquitto.conf", "/mosquitto/config/mosquitto.conf", org.testcontainers.containers.BindMode.READ_ONLY);

    private static String brokerUrl() {
        return "tcp://" + mosquittoContainer.getHost() + ":" + mosquittoContainer.getMappedPort(1883);
    }

    @DynamicPropertySource
    static void mqttProperties(DynamicPropertyRegistry registry) {
        // Configuro i parametri di connessione dell'Edge affinché punti al broker isolato avviato nel container.
        registry.add("bitpub.mqtt.local.broker-url", EdgeMqttIntegrationTest::brokerUrl);
    }

    @Test
    void validSensorEventIsForwardedFromEdgeToCloud() throws Exception {
        String gameInstanceId = "gi-" + UUID.randomUUID();
        String localSensorTopic = MqttTopics.getSensorEventTopic("locale-1", gameInstanceId);
        String cloudIngestTopic = MqttTopics.getCloudSensorIngestTopic(gameInstanceId);
        ObjectMapper mapper = new ObjectMapper();

        MqttClient probe = new MqttClient(brokerUrl(), "e2e-probe-" + UUID.randomUUID(), new MemoryPersistence());
        probe.connect();
        try {
            CountDownLatch received = new CountDownLatch(1);
            AtomicReference<String> forwarded = new AtomicReference<>();
            probe.subscribe(cloudIngestTopic, (t, msg) -> {
                forwarded.set(new String(msg.getPayload()));
                received.countDown();
            });

            // Simulo un evento di gioco valido, in questo caso ometto il matchId per testare il puro instradamento.
            UUID eventId = UUID.randomUUID();
            String json = mapper.writeValueAsString(Map.of(
                    "eventId", eventId.toString(),
                    "gameInstanceId", gameInstanceId,
                    "sensorType", "GOAL"));

            // Visto che l'Edge potrebbe richiedere qualche istante per connettersi all'avvio,
            // continuo a pubblicare il messaggio finché non viene ricevuto, sfruttando l'idempotenza basata sull'eventId.
            for (int i = 0; i < 20 && received.getCount() > 0; i++) {
                probe.publish(localSensorTopic, new MqttMessage(json.getBytes()));
                if (received.await(500, TimeUnit.MILLISECONDS)) break;
            }

            assertTrue(received.getCount() == 0, "Cloud non ha ricevuto l'evento inoltrato dall'Edge");
            assertEquals(gameInstanceId, mapper.readTree(forwarded.get()).path("gameInstanceId").asText());
            assertEquals(eventId.toString(), mapper.readTree(forwarded.get()).path("eventId").asText());
        } finally {
            probe.disconnect();
            probe.close();
        }
    }
}
