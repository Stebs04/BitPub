package com.bitpub.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Importiamo Awaitility e classi per gestire il tempo
import org.awaitility.Awaitility;
import java.util.concurrent.TimeUnit;
import java.util.List;

import com.bitpub.repository.PartitaFreccetteRepository;
import com.bitpub.models.PartitaFreccette;

@SpringBootTest
@ActiveProfiles("test")
public class MqttToDbIntegrationTest {

    @Autowired
    private ElaborazioneEventiService elaborazioneEventiService;

    @Autowired
    private PartitaFreccetteRepository freccetteRepository;

    @BeforeEach
    void setUp() {
        freccetteRepository.deleteAll();
    }

    @Test
    public void testSalvataggioDaMqttADatabase() {

        String topicWildcard = "bitpub/locali/1/freccette/bersaglio1/eventi";
        String jsonPayload = "{\"giocatoreVincitore\": \"Stefano\", \"punteggio\": 301, \"mosse\": 15}";

        // 2. ESECUZIONE: Questo metodo lancia un thread asincrono (@Async)
        elaborazioneEventiService.processaESalvaEvento(topicWildcard, jsonPayload);

        // 3. VERIFICA ASINCRONA:
        // Diciamo ad Awaitility di aspettare al massimo 5 secondi.
        // Continuerà a controllare il database ogni 100ms finché la condizione non è vera.
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> freccetteRepository.findAll().size() == 1);

        // A questo punto siamo sicuri che il thread in background ha finito!
        // Possiamo fare le nostre asserzioni standard
        List<PartitaFreccette> partiteSalvate = freccetteRepository.findAll();
        assertEquals(1, partiteSalvate.size(), "Deve esserci una sola partita salvata nel DB");
        assertEquals("Stefano", partiteSalvate.get(0).getGiocatoreVincitore());
    }
}