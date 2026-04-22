package com.bitpub.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;

@SpringBootTest
public class MqttToDbIntegrationTest {

    // Sostituisci questo con il nome reale del tuo servizio che gestisce i messaggi
    @Autowired
    private ElaborazioneEventiService elaborazioneEventiService;

    @Autowired
    private PartitaFreccetteRepository freccetteRepository;

    @BeforeEach
    void setUp() {
        freccetteRepository.deleteAll(); // Pulizia iniziale
    }

    @Test
    public void testSalvataggioDaMqttADatabase() {
        // 1. PREPARAZIONE: Simuliamo i dati che arriverebbero da Mosquitto (Edge Node)
        String topicWildcard = "bitpub/locali/1/freccette/bersaglio1/eventi";
        // Simuliamo un JSON serializzato (come farebbe GSON sull'Edge)
        String jsonPayload = "{\"giocatoreVincitore\": \"Stefano\", \"punteggio\": 301, \"mosse\": 15}";

        // 2. ESECUZIONE: Chiamiamo il metodo che normalmente scatta al `messageArrived`
        elaborazioneEventiService.elaboraMessaggio(topicWildcard, jsonPayload);

        // 3. VERIFICA: Controlliamo se il DAO ha scritto fisicamente su PostgreSQL (o DB in memoria)
        List<PartitaFreccette> partiteSalvate = freccetteRepository.findAll();

        // Ci aspettiamo esattamente 1 record
        assertEquals(1, partiteSalvate.size(), "Deve esserci una sola partita salvata nel DB");
        // Verifichiamo che i dati siano stati mappati e salvati correttamente
        assertEquals("Stefano", partiteSalvate.get(0).getGiocatoreVincitore());
    }
}