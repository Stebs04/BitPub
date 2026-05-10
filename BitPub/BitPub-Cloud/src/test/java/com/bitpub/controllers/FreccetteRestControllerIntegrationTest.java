package com.bitpub.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.bitpub.repository.PartitaFreccetteRepository;
import com.bitpub.models.PartitaFreccette;

@SpringBootTest // Carica tutto il contesto dell'applicazione Spring
@AutoConfigureMockMvc // Configura MockMvc per simulare le chiamate HTTP
@ActiveProfiles("test") // Indica a Spring di usare il DB in memoria definito in application-test.properties
public class FreccetteRestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // Il nostro "finto" client web

    @Autowired
    private PartitaFreccetteRepository freccetteRepository; // Il nostro DAO per il DB

    @BeforeEach
    void setUp() {
        // Puliamo il database prima di ogni test per avere un ambiente pulito
        freccetteRepository.deleteAll();
    }

    @Test
    public void testGetPartiteRestDbIntegration() throws Exception {
        // 1. PREPARAZIONE: Inseriamo una partita fittizia direttamente nel DB
        PartitaFreccette partita = new PartitaFreccette();
        partita.setGiocatoreVincitore("Timothy");
        partita.setPunteggio(501);
        freccetteRepository.save(partita);

        // 2. ESECUZIONE E VERIFICA: Simuliamo la chiamata GET alla nostra API REST
        mockMvc.perform(get("/api/v1/freccette/partite") // Assicurati che l'URI sia quello giusto!
                        .contentType(MediaType.APPLICATION_JSON))
                // Verifichiamo che lo stato HTTP sia 200 OK
                .andExpect(status().isOk())
                // Verifichiamo che il JSON restituito contenga il vincitore giusto
                .andExpect(jsonPath("$[0].giocatoreVincitore").value("Timothy"))
                .andExpect(jsonPath("$[0].punteggio").value(501));
    }
}