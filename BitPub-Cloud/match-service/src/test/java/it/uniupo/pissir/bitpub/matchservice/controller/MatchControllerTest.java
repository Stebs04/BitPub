package it.uniupo.pissir.bitpub.matchservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.GlobalExceptionHandler;
import it.uniupo.pissir.bitpub.matchservice.dto.JoinLobbyRequestDto;
import it.uniupo.pissir.bitpub.matchservice.dto.MatchDto;
import it.uniupo.pissir.bitpub.matchservice.dto.StartMatchRequestDto;
import it.uniupo.pissir.bitpub.matchservice.service.impl.MatchServiceImpl;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GlobalExceptionHandler importato: mappa BitpubException sui codici HTTP (400/403) usati dal controller.
@WebMvcTest(MatchController.class)
@Import(GlobalExceptionHandler.class)
class MatchControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private MatchServiceImpl matchService;

    @Test
    void startMatch_returns201() throws Exception {
        when(matchService.startMatch(any())).thenReturn(MatchDto.builder().id("m1").status("IN_PROGRESS").build());
        StartMatchRequestDto req = new StartMatchRequestDto();
        req.setGameInstanceId("gi1");

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("m1"));
    }

    @Test
    void joinLobby_missingUserIdHeader_returns400() throws Exception {
        JoinLobbyRequestDto req = new JoinLobbyRequestDto("gi1", "bob", null);
        mockMvc.perform(post("/api/matches/lobby")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinLobby_valid_returns200() throws Exception {
        when(matchService.joinLobby(any(), eq("pB"))).thenReturn(MatchDto.builder().id("m1").status("IN_PROGRESS").build());
        JoinLobbyRequestDto req = new JoinLobbyRequestDto("gi1", "bob", null);

        mockMvc.perform(post("/api/matches/lobby")
                        .header("X-User-Id", "pB")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void endMatch_nonAdminRole_returns403() throws Exception {
        mockMvc.perform(post("/api/matches/m1/end")
                        .header("X-User-Role", "PLAYER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActiveMatches_openAccess_returns200() throws Exception {
        when(matchService.getActiveMatches()).thenReturn(java.util.List.of(MatchDto.builder().id("m1").build()));
        mockMvc.perform(get("/api/matches/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("m1"));
    }
}
