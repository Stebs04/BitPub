package it.uniupo.pissir.bitpub.tournamentservice.controller;

import it.uniupo.pissir.bitpub.tournamentservice.dto.TournamentRankingDto;
import it.uniupo.pissir.bitpub.tournamentservice.service.TournamentRankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TournamentRankingController.class)
class TournamentRankingControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TournamentRankingService rankingService;

    @Test
    void getTournamentRankings_returns200List() throws Exception {
        when(rankingService.getTournamentRankings("t1"))
                .thenReturn(List.of(TournamentRankingDto.builder().participantId("a").currentRank(1).build()));

        mockMvc.perform(get("/api/v1/tournaments/t1/rankings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participantId").value("a"))
                .andExpect(jsonPath("$[0].currentRank").value(1));
    }

    @Test
    void updateRankingScore_returns200() throws Exception {
        when(rankingService.updateRankingScore("t1", "a", 3, true))
                .thenReturn(TournamentRankingDto.builder().participantId("a").matchesWon(1).build());

        mockMvc.perform(put("/api/v1/tournaments/t1/rankings/score")
                        .param("participantId", "a").param("scoreDelta", "3").param("isWin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchesWon").value(1));
    }
}
