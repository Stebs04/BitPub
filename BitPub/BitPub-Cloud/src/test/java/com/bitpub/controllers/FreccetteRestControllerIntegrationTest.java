package com.bitpub.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
// Importiamo l'annotazione per simulare un utente autenticato
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.bitpub.repository.PartitaFreccetteRepository;
import com.bitpub.models.PartitaFreccette;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FreccetteRestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PartitaFreccetteRepository freccetteRepository;

    @BeforeEach
    void setUp() {
        freccetteRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin_test", roles = {"ADMIN"})
    public void testGetPartiteRestDbIntegration() throws Exception {
        // 1. PREPARAZIONE
        PartitaFreccette partita = new PartitaFreccette();
        partita.setGiocatoreVincitore("Timothy");
        partita.setPunteggio(501);
        freccetteRepository.save(partita);

        // 2. ESECUZIONE E VERIFICA
        mockMvc.perform(get("/api/v1/freccette/partite")
                        .contentType(MediaType.APPLICATION_JSON)
                        // Aggiungiamo l'header Accept richiesto dal tuo ApiVersionFilter!
                        .accept("application/resources.v1+json"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].giocatoreVincitore").value("Timothy"))
                .andExpect(jsonPath("$[0].punteggio").value(501));

    }
}