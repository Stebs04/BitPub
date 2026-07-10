/**
 * Autore: Stefano Bellan Matricola 20054330
 */
package it.uniupo.pissir.bitpub.gamecatalogservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.AddSensorRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.CreateGameTypeRequest;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.GameTypeDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.dto.SensorDefinitionDto;
import it.uniupo.pissir.bitpub.gamecatalogservice.service.GameCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test unitari focalizzati sulle regole di validazione e la serializzazione delle risposte REST.
 * L'inibizione dei filtri di sicurezza globali consente di validare puntualmente le autorizzazioni
 * tramite profili utente simulati (MockUser).
 */
@WebMvcTest(GameCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameCatalogControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private GameCatalogService gameCatalogService;

    @Test
    @WithMockUser(roles = "GAME_ADMIN")
    void createGameType_valid_returns201() throws Exception {
        CreateGameTypeRequest req = new CreateGameTypeRequest();
        req.setName("Pool"); req.setDescription("d");
        when(gameCatalogService.createGameType(any())).thenReturn(GameTypeDto.builder().id("g1").name("Pool").build());

        mockMvc.perform(post("/api/v1/catalog/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("g1"));
    }

    @Test
    @WithMockUser(roles = "GAME_ADMIN")
    void createGameType_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllGameTypes_returns200List() throws Exception {
        when(gameCatalogService.getAllGameTypes()).thenReturn(List.of(GameTypeDto.builder().id("g1").build()));
        mockMvc.perform(get("/api/v1/catalog/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("g1"));
    }

    @Test
    @WithMockUser(roles = "GAME_ADMIN")
    void addSensor_valid_returns201() throws Exception {
        when(gameCatalogService.addSensorToGameType(eq("g1"), any())).thenReturn(SensorDefinitionDto.builder().id("s1").build());
        AddSensorRequest req = new AddSensorRequest();
        req.setType("goal"); req.setDescription("d");

        mockMvc.perform(post("/api/v1/catalog/games/g1/sensors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("s1"));
    }

    @Test
    @WithMockUser(roles = "GAME_ADMIN")
    void deleteGameType_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/catalog/games/g1"))
                .andExpect(status().isNoContent());
    }
}
