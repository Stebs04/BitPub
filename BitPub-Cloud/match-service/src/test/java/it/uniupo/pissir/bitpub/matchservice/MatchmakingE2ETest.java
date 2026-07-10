// Autore: Timothy Giolito 20054431
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
 * FASE 5 — Scenario 2: Matchmaking.
 * Il primo giocatore che effettua l'accesso alla lobby per una specifica istanza di gioco
 * inizializza una partita in stato WAITING_FOR_PLAYERS. L'ingresso di un secondo giocatore
 * innesca il passaggio immediato allo stato IN_PROGRESS.
 * L'infrastruttura di test garantisce un test end-to-e (E2E) tramite protocollo HTTP 
 * sfruttando un database PostgreSQL autentico (via Testcontainers). L'unica dipendenza 
 * esterna (locale-service), necessaria alla validazione dello stato operativo della macchina, 
 * è surrogata da uno stub HTTP basato sulle librerie JDK standard.
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
        // Simulazione endpoint GET /api/v1/locales/games/{id}: certifica che la postazione sia attiva
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
        // Assenza volontaria di broker MQTT nel test: gli adapter si limiteranno a iterare
        // i tentativi di connessione su un indirizzo inerte senza ostacolare l'avvio (boot) del contesto.
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

        // Primo giocatore: inizializzazione della lobby in attesa
        ResponseEntity<MatchDto> first = joinLobby("user-a", "alice", gi);
        assertEquals(HttpStatus.OK, first.getStatusCode());
        MatchDto lobby = first.getBody();
        assertNotNull(lobby);
        assertEquals("WAITING_FOR_PLAYERS", lobby.getStatus());
        assertEquals(1, lobby.getTeams().size());

        // Secondo giocatore: la lobby precedentemente istanziata passa in IN_PROGRESS; 
        // il primo turno viene conferito al creatore della sessione.
        ResponseEntity<MatchDto> second = joinLobby("user-b", "bob", gi);
        assertEquals(HttpStatus.OK, second.getStatusCode());
        MatchDto started = second.getBody();
        assertNotNull(started);
        assertEquals(lobby.getId(), started.getId(), "Deve corrispondere alla medesima istanza della partita");
        assertEquals("IN_PROGRESS", started.getStatus());
        assertEquals(2, started.getTeams().size());
        assertEquals("user-a", started.getCurrentTurnUserId());
    }
}
