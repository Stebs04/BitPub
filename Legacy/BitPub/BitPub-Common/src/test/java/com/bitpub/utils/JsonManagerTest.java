package com.bitpub.utils;

import com.bitpub.models.Partita;
import com.bitpub.models.PartitaBiliardo;
import com.bitpub.models.PartitaCalciobalilla;
import com.bitpub.models.PartitaFreccette;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonManager and PartitaDeserializer.
 * Verifies JSON serialization and polymorphic deserialization.
 *
 * @author Stefano Bellan 20054330
 */
public class JsonManagerTest {

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = JsonManager.getGson();
    }

    @Test
    void testDeserializePartitaBiliardo() {
        String json = "{ \"tipoGioco\": \"BILIARDO\", \"specialita\": \"Palla 8\", \"serieMassimaPalleImbucate\": 5, \"falli\": 2 }";
        Partita partita = gson.fromJson(json, Partita.class);

        assertNotNull(partita);
        assertTrue(partita instanceof PartitaBiliardo);
        
        PartitaBiliardo pb = (PartitaBiliardo) partita;
        assertEquals("BILIARDO", pb.getTipoGioco());
        assertEquals("Palla 8", pb.getSpecialita());
        assertEquals(5, pb.getSerieMassimaPalleImbucate());
        assertEquals(2, pb.getFalli());
    }

    @Test
    void testDeserializePartitaCalciobalilla() {
        String json = "{ \"tipoGioco\": \"CALCIOBALILLA\", \"totaleGol\": 10, \"totaleRullate\": 2, \"durataMediaPallinaSecondi\": 30, \"goalRossi\": 6, \"goalBlu\": 4 }";
        Partita partita = gson.fromJson(json, Partita.class);

        assertNotNull(partita);
        assertTrue(partita instanceof PartitaCalciobalilla);
        
        PartitaCalciobalilla pc = (PartitaCalciobalilla) partita;
        assertEquals("CALCIOBALILLA", pc.getTipoGioco());
        assertEquals(10, pc.getTotaleGol());
        assertEquals(6, pc.getGoalRossi());
        assertEquals(2, pc.getTotaleRullate());
    }

    @Test
    void testDeserializePartitaFreccette() {
        String json = "{ \"tipoGioco\": \"FRECCETTE\", \"modalita\": \"501\", \"numero180\": 2, \"percentualeBullseye\": 15.5 }";
        Partita partita = gson.fromJson(json, Partita.class);

        assertNotNull(partita);
        assertTrue(partita instanceof PartitaFreccette);
        
        PartitaFreccette pf = (PartitaFreccette) partita;
        assertEquals("FRECCETTE", pf.getTipoGioco());
        assertEquals("501", pf.getModalita());
        assertEquals(2, pf.getNumero180());
        assertEquals(15.5, pf.getPercentualeBullseye(), 0.01);
    }

    @Test
    void testDeserializeUnknownPartitaThrowsException() {
        String json = "{ \"tipoGioco\": \"PingPong\" }";
        assertThrows(JsonParseException.class, () -> gson.fromJson(json, Partita.class));
    }

    @Test
    void testDeserializePartitaWithoutTipoGiocoThrowsException() {
        String json = "{ \"totaleGol\": 10 }";
        assertThrows(JsonParseException.class, () -> gson.fromJson(json, Partita.class));
    }
}
