package com.bitpub.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Partita subclasses (Biliardo, Calciobalilla, Freccette).
 * Verifies game state initialization, score logic, and default values.
 * 
 * @author Stefano Bellan 20054330
 */
public class PartitaTest {

    @Test
    void testPartitaBiliardoInitialization() {
        PartitaBiliardo partita = new PartitaBiliardo("Palla 8", 5, 2);
        
        assertEquals("BILIARDO", partita.getTipoGioco(), "Gameplay type should be strictly set to BILIARDO");
        assertEquals("Palla 8", partita.getSpecialita());
        assertEquals(5, partita.getSerieMassimaPalleImbucate());
        assertEquals(2, partita.getFalli());
    }

    @Test
    void testPartitaCalciobalillaScoring() {
        PartitaCalciobalilla partita = new PartitaCalciobalilla(10, 3, 45, 6, 4);
        
        assertEquals("CALCIOBALILLA", partita.getTipoGioco(), "Gameplay type should be strictly set to CALCIOBALILLA");
        assertEquals(10, partita.getTotaleGol());
        assertEquals(6, partita.getGoalRossi());
        assertEquals(4, partita.getGoalBlu());
        assertEquals(3, partita.getTotaleRullate());
        assertEquals(45, partita.getDurataMediaPallinaSecondi());
        
        // Test updating scores
        partita.setGoalRossi(7);
        assertEquals(7, partita.getGoalRossi());
        partita.setTotaleGol(11);
        assertEquals(11, partita.getTotaleGol());
    }

    @Test
    void testPartitaFreccetteValidation() {
        PartitaFreccette partita = new PartitaFreccette("501", 3, 20.5);
        
        assertEquals("FRECCETTE", partita.getTipoGioco(), "Gameplay type should be strictly set to FRECCETTE");
        assertEquals("501", partita.getModalita());
        assertEquals(3, partita.getNumero180());
        assertEquals(20.5, partita.getPercentualeBullseye(), 0.01);
        
        // Validate updating precision
        partita.setPercentualeBullseye(35.0);
        assertEquals(35.0, partita.getPercentualeBullseye(), 0.01);
    }
}
