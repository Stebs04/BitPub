package it.uniupo.pissir.bitpub.edge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GameManagementServiceTest {

    private GameManagementService service;

    @BeforeEach
    void setUp() {
        service = new GameManagementService();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> registry() {
        return (Map<String, String>) ReflectionTestUtils.getField(service, "installedGames");
    }

    @Test
    void addEvent_registersGameInstance() {
        service.handleGameEvent(new GenericMessage<>(
                "{\"action\":\"ADD\",\"gameInstanceId\":\"gi1\",\"gameTypeId\":\"pool\"}"));

        assertThat(registry()).containsEntry("gi1", "pool");
    }

    @Test
    void removeEvent_unregistersGameInstance() {
        registry().put("gi1", "pool");

        service.handleGameEvent(new GenericMessage<>(
                "{\"action\":\"REMOVE\",\"gameInstanceId\":\"gi1\"}"));

        assertThat(registry()).doesNotContainKey("gi1");
    }

    @Test
    void malformedPayload_isSwallowed() {
        assertThatCode(() -> service.handleGameEvent(new GenericMessage<>("not-json")))
                .doesNotThrowAnyException();
        assertThat(registry()).isEmpty();
    }
}
