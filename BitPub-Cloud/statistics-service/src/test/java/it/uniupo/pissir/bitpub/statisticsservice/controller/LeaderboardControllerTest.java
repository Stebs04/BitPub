package it.uniupo.pissir.bitpub.statisticsservice.controller;

import it.uniupo.pissir.bitpub.common.exception.GlobalExceptionHandler;
import it.uniupo.pissir.bitpub.statisticsservice.dto.LeaderboardEntryDto;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaderboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class LeaderboardControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private StatisticsService statisticsService;

    @Test
    void getLeaderboard_returns200Ordered() throws Exception {
        when(statisticsService.getLeaderboard("pool"))
                .thenReturn(List.of(LeaderboardEntryDto.builder().playerName("alice").wins(3).build()));

        mockMvc.perform(get("/api/v1/statistics/leaderboard/pool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerName").value("alice"))
                .andExpect(jsonPath("$[0].wins").value(3));
    }

    @Test
    void getLeaderboardByLocale_localeAdminMismatch_returns403() throws Exception {
        when(statisticsService.resolveAdminLocaleId("admin1")).thenReturn("loc1");

        mockMvc.perform(get("/api/v1/statistics/leaderboard/locale/otherLoc/pool")
                        .header("X-User-Id", "admin1")
                        .header("X-User-Role", "LOCALE_ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLeaderboardByLocale_localeAdminOwnLocale_returns200() throws Exception {
        when(statisticsService.resolveAdminLocaleId("admin1")).thenReturn("loc1");
        when(statisticsService.getLeaderboardByLocale("pool", "loc1"))
                .thenReturn(List.of(LeaderboardEntryDto.builder().playerName("bob").build()));

        mockMvc.perform(get("/api/v1/statistics/leaderboard/locale/loc1/pool")
                        .header("X-User-Id", "admin1")
                        .header("X-User-Role", "LOCALE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerName").value("bob"));
    }
}
