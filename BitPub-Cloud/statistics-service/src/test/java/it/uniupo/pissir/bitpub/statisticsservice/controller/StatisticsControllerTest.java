package it.uniupo.pissir.bitpub.statisticsservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.common.exception.GlobalExceptionHandler;
import it.uniupo.pissir.bitpub.statisticsservice.dto.AggregateStatisticDto;
import it.uniupo.pissir.bitpub.statisticsservice.dto.GlobalStatsDto;
import it.uniupo.pissir.bitpub.statisticsservice.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StatisticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private StatisticsService statisticsService;

    @Test
    void getStatistics_localeAdminOwnLocale_returns200() throws Exception {
        when(statisticsService.getStatisticsByEntity("loc1", "LOCALE"))
                .thenReturn(List.of(AggregateStatisticDto.builder().entityId("loc1").build()));

        mockMvc.perform(get("/api/v1/statistics")
                        .param("entityId", "loc1").param("entityType", "LOCALE")
                        .header("X-User-Role", "LOCALE_ADMIN")
                        .header("X-User-Locale-Id", "loc1"))
                .andExpect(status().isOk());
    }

    @Test
    void getStatistics_localeAdminOtherLocale_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/statistics")
                        .param("entityId", "otherLoc").param("entityType", "LOCALE")
                        .header("X-User-Role", "LOCALE_ADMIN")
                        .header("X-User-Locale-Id", "loc1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void getGlobalOverview_platformAdmin_returns200() throws Exception {
        when(statisticsService.getGlobalOverview()).thenReturn(GlobalStatsDto.builder().totalUsers(5L).build());

        mockMvc.perform(get("/api/v1/statistics/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(5));
    }
}
