package it.uniupo.pissir.bitpub.tournamentservice;

import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentMatchDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRegistrationDto;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FASE 5 — Scenario 3: generazione automatica del tabellone e avanzamento del vincitore.
 * Creato un torneo da 4 posti, alla quarta iscrizione il tabellone (2 semifinali + finale) si
 * genera da solo. Riportando l'esito di ciascuna semifinale, il vincitore avanza in finale.
 * E2E completo via HTTP contro un Postgres reale (Testcontainers). tournament-service non ha
 * dipendenze REST esterne, quindi nessuno stub serve.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class TournamentBracketE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Nessun broker MQTT nel test: indirizzo morto, gli adapter ritentano senza bloccare il boot.
        registry.add("mqtt.broker-url", () -> "tcp://localhost:1");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders headers(String role, String localeId) {
        HttpHeaders h = new HttpHeaders();
        h.add("X-User-Role", role);
        if (localeId != null) h.add("X-User-Locale-Id", localeId);
        return h;
    }

    @Test
    void bracketAutoGeneratesAndWinnersAdvanceToFinal() {
        // 1. LOCALE_ADMIN crea un torneo da 4 posti.
        TournamentDto create = new TournamentDto();
        create.setName("Coppa E2E");
        create.setGameTypeId("foosball");
        create.setMaxParticipants(4);
        create.setTeamBased(false);
        ResponseEntity<TournamentDto> created = restTemplate.exchange(
                "/api/v1/tournaments", HttpMethod.POST,
                new HttpEntity<>(create, headers("LOCALE_ADMIN", "locale-1")), TournamentDto.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        String tid = created.getBody().getId();

        // 2. Quattro PLAYER si iscrivono; alla quarta il tabellone si genera.
        for (int i = 1; i <= 4; i++) {
            TournamentRegistrationDto reg = new TournamentRegistrationDto();
            reg.setParticipantId("p" + i);
            reg.setParticipantName("P" + i);
            reg.setLocaleId("locale-1");
            reg.setTeam(false);
            reg.setMembers(List.of("P" + i));
            ResponseEntity<TournamentRegistrationDto> r = restTemplate.exchange(
                    "/api/v1/tournaments/" + tid + "/register", HttpMethod.POST,
                    new HttpEntity<>(reg, headers("PLAYER", null)), TournamentRegistrationDto.class);
            assertEquals(HttpStatus.CREATED, r.getStatusCode());
        }

        // 3. Tabellone generato: 2 semifinali (round 0) + 1 finale (round 1).
        TournamentDto withBracket = getTournament(tid);
        List<TournamentMatchDto> bracket = withBracket.getBracket();
        assertNotNull(bracket);
        assertEquals(3, bracket.size(), "4 giocatori => 2 semifinali + 1 finale");
        List<TournamentMatchDto> semis = bracket.stream().filter(m -> m.getRound() == 0).toList();
        TournamentMatchDto finalMatch = bracket.stream().filter(m -> m.getRound() == 1).findFirst().orElseThrow();
        assertEquals(2, semis.size());
        assertNull(finalMatch.getPlayer1Id(), "la finale parte senza giocatori");
        assertNull(finalMatch.getPlayer2Id());

        // 4. Riporto l'esito di entrambe le semifinali: vince il player1 di ciascuna.
        for (TournamentMatchDto semi : semis) {
            ResponseEntity<Void> res = restTemplate.exchange(
                    "/api/v1/tournaments/matches/" + semi.getId() + "/result?winnerId=" + semi.getPlayer1Id(),
                    HttpMethod.POST, new HttpEntity<>(null, headers("PLAYER", null)), Void.class);
            assertEquals(HttpStatus.OK, res.getStatusCode());
        }

        // 5. I due vincitori sono avanzati in finale.
        TournamentMatchDto advancedFinal = getTournament(tid).getBracket().stream()
                .filter(m -> m.getRound() == 1).findFirst().orElseThrow();
        assertNotNull(advancedFinal.getPlayer1Id(), "primo vincitore avanzato in finale");
        assertNotNull(advancedFinal.getPlayer2Id(), "secondo vincitore avanzato in finale");
        List<String> expectedFinalists = semis.stream().map(TournamentMatchDto::getPlayer1Id).toList();
        assertTrue(expectedFinalists.contains(advancedFinal.getPlayer1Id()));
        assertTrue(expectedFinalists.contains(advancedFinal.getPlayer2Id()));
    }

    private TournamentDto getTournament(String id) {
        ResponseEntity<TournamentDto> r = restTemplate.getForEntity("/api/v1/tournaments/" + id, TournamentDto.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        return r.getBody();
    }
}
