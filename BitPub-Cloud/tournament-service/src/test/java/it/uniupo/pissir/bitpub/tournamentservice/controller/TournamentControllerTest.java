/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.tournamentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.GlobalExceptionHandler;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentDto;
import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRegistrationDto;
import it.uniupo.pissir.bitpub.tournamentservice.service.impl.TournamentServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite di test unitari per il controller dei tornei.
 * Verifica i vincoli di accesso basati sui ruoli e la corretta mappatura degli endpoint HTTP.
 */
@WebMvcTest(TournamentController.class)
@Import(GlobalExceptionHandler.class)
class TournamentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TournamentServiceImpl tournamentService;

    @Test
    void createTournament_nonLocaleAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/tournaments")
                        .header("X-User-Role", "PLAYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TournamentDto.builder().name("T").build())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTournament_localeAdmin_returns201() throws Exception {
        when(tournamentService.createTournament(any())).thenReturn(TournamentDto.builder().id("t1").name("T").build());

        mockMvc.perform(post("/api/v1/tournaments")
                        .header("X-User-Role", "LOCALE_ADMIN")
                        .header("X-User-Locale-Id", "loc1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TournamentDto.builder().name("T").build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("t1"));
    }

    @Test
    void register_nonPlayer_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/tournaments/t1/register")
                        .header("X-User-Role", "LOCALE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TournamentRegistrationDto.builder().participantId("a").build())))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_player_returns201() throws Exception {
        when(tournamentService.registerToTournament(eq("t1"), any()))
                .thenReturn(TournamentRegistrationDto.builder().id("r1").participantId("a").build());

        mockMvc.perform(post("/api/v1/tournaments/t1/register")
                        .header("X-User-Role", "PLAYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TournamentRegistrationDto.builder().participantId("a").build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("r1"));
    }

    @Test
    void authorizeBracketPlayer_returnsBoolean() throws Exception {
        when(tournamentService.isPlayerInBracketMatch("m1", "a")).thenReturn(true);
        mockMvc.perform(get("/api/v1/tournaments/matches/m1/authorize").param("playerId", "a"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
