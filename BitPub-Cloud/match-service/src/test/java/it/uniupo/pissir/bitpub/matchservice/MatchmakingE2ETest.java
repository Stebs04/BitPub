package it.uniupo.pissir.bitpub.matchservice;

import com.sun.net.httpserver.HttpServer;
import it.uniupo.pissir.bitpub.matchservice.dto.JoinLobbyRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FASE 5 — Scenario 2: matchmaking. Il primo giocatore che entra in lobby su una gameInstance
 * crea una partita WAITING_FOR_PLAYERS; il secondo la porta IN_PROGRESS in tempo reale.
 * E2E completo via HTTP contro un Postgres reale (Testcontainers). La sola dipendenza esterna
 * usata dal path (locale-service, per validare che la gameInstance sia attiva) e' sostituita
 * da uno stub HTTP JDK — nessuna nuova libreria.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class MatchmakingE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    static HttpServer localeStub;

    @BeforeAll
    static void startLocaleStub() throws IOException {
        localeStub = HttpServer.create(new InetSocketAddress(0), 0);
        // GET /api/v1/locales/games/{id}: la macchina e' attiva nel locale-1.
        localeStub.createContext("/api/v1/locales/games/", exchange -> {
            byte[] body = ("{\"active\":true,\"localeId\":\"locale-1\",\"gameTypeId\":\"foosball\",\"localInstanceId\":\"calcio-1\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        localeStub.start();
    }

    @AfterAll
    static void stopLocaleStub() {
        localeStub.stop(0);
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("locale.service.url", () -> "http://localhost:" + localeStub.getAddress().getPort());
        // Nessun broker MQTT nel test: indirizzo morto, gli adapter ritentano senza bloccare il boot.
        registry.add("mqtt.broker-url", () -> "tcp://localhost:1");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<MatchDto> joinLobby(String userId, String username, String gameInstanceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", userId);
        JoinLobbyRequestDto body = JoinLobbyRequestDto.builder()
                .gameInstanceId(gameInstanceId).username(username).build();
        return restTemplate.exchange("/api/matches/lobby", HttpMethod.POST,
                new HttpEntity<>(body, headers), MatchDto.class);
    }

    @Test
    void secondPlayerJoiningTransitionsLobbyToInProgress() {
        String gi = "gi-e2e-1";

        // Primo giocatore: crea la lobby in attesa.
        ResponseEntity<MatchDto> first = joinLobby("user-a", "alice", gi);
        assertEquals(HttpStatus.OK, first.getStatusCode());
        MatchDto lobby = first.getBody();
        assertNotNull(lobby);
        assertEquals("WAITING_FOR_PLAYERS", lobby.getStatus());
        assertEquals(1, lobby.getTeams().size());

        // Secondo giocatore: la stessa lobby passa IN_PROGRESS con il turno sul primo giocatore.
        ResponseEntity<MatchDto> second = joinLobby("user-b", "bob", gi);
        assertEquals(HttpStatus.OK, second.getStatusCode());
        MatchDto started = second.getBody();
        assertNotNull(started);
        assertEquals(lobby.getId(), started.getId(), "stessa partita, non una nuova lobby");
        assertEquals("IN_PROGRESS", started.getStatus());
        assertEquals(2, started.getTeams().size());
        assertEquals("user-a", started.getCurrentTurnUserId());
    }
}
