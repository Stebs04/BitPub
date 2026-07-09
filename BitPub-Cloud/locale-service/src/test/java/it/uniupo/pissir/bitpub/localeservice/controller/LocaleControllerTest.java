package it.uniupo.pissir.bitpub.localeservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.localeservice.dto.AddGameInstanceRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.CreateLocaleRequest;
import it.uniupo.pissir.bitpub.localeservice.dto.GameInstanceDto;
import it.uniupo.pissir.bitpub.localeservice.dto.LocaleDto;
import it.uniupo.pissir.bitpub.localeservice.service.LocaleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// addFilters=false: la security a livello di metodo (@PreAuthorize) e' testata sul service
// (assertLocaleManageable); qui si verifica solo il mapping HTTP e il passaggio degli header.
@WebMvcTest(LocaleController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocaleControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private LocaleService localeService;

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN") // createLocale e' @PreAuthorize('PLATFORM_ADMIN')
    void createLocale_valid_returns201() throws Exception {
        CreateLocaleRequest req = new CreateLocaleRequest();
        req.setName("Bar"); req.setAddress("Via 1"); req.setAdminId("admin1");
        when(localeService.createLocale(any())).thenReturn(LocaleDto.builder().id("loc1").name("Bar").build());

        mockMvc.perform(post("/api/v1/locales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("loc1"));
    }

    @Test
    void createLocale_blankFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/locales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"address\":\"\",\"adminId\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addGameInstance_forwardsCallerHeadersToService() throws Exception {
        when(localeService.addGameInstance(eq("loc1"), any(), eq("admin1"), eq("LOCALE_ADMIN")))
                .thenReturn(GameInstanceDto.builder().id("gi1").build());
        AddGameInstanceRequest req = new AddGameInstanceRequest();
        req.setLocalInstanceId("m1"); req.setGameTypeId("pool");

        mockMvc.perform(post("/api/v1/locales/loc1/games")
                        .header("X-User-Id", "admin1")
                        .header("X-User-Role", "LOCALE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("gi1"));

        ArgumentCaptor<String> role = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(localeService).addGameInstance(eq("loc1"), any(), eq("admin1"), role.capture());
        assertThat(role.getValue()).isEqualTo("LOCALE_ADMIN");
    }

    @Test
    void getOnlineLocales_returns200List() throws Exception {
        when(localeService.getOnlineLocales()).thenReturn(List.of(LocaleDto.builder().id("on").build()));
        mockMvc.perform(get("/api/v1/locales/online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("on"));
    }

    @Test
    void removeGameInstance_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/locales/loc1/games/gi1")
                        .header("X-User-Id", "admin1")
                        .header("X-User-Role", "LOCALE_ADMIN"))
                .andExpect(status().isNoContent());
    }
}
