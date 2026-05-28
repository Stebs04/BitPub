package com.bitpub.simulator.service;

import com.bitpub.domain.PartitaCalciobalilla;
import com.bitpub.domain.PartitaBiliardo;
import com.bitpub.domain.PartitaFreccette;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class SimulatorService {

    private final Random random = new Random();

    public PartitaCalciobalilla simulateCalciobalilla() {
        PartitaCalciobalilla p = new PartitaCalciobalilla(0, 0, 0, 0, 0);
        p.setOrarioInizio(LocalDateTime.now());
        
        int goalRossi = 0;
        int goalBlu = 0;
        int totaleRullate = 0;
        
        while (goalRossi < 10 && goalBlu < 10) {
            int r = random.nextInt(100);
            if (r < 40) goalRossi++;
            else if (r < 80) goalBlu++;
            else totaleRullate++;
        }
        
        p.setGoalRossi(goalRossi);
        p.setGoalBlu(goalBlu);
        p.setTotaleRullate(totaleRullate);
        p.setTotaleGol(goalRossi + goalBlu);
        p.setDurataMediaPallinaSecondi(10 + random.nextInt(20));
        p.setOrarioFine(LocalDateTime.now().plusMinutes(5)); // Simuliamo 5 minuti di partita
        return p;
    }

    public PartitaBiliardo simulateBiliardo() {
        PartitaBiliardo p = new PartitaBiliardo();
        p.setOrarioInizio(LocalDateTime.now());
        
        int palleImbucate = 0;
        int falli = 0;
        
        while (palleImbucate < 8) {
            int r = random.nextInt(100);
            if (r < 70) palleImbucate++;
            else falli++;
        }
        
        p.setSpecialita("Palla 8");
        p.setSerieMassimaPalleImbucate(random.nextInt(4) + 1);
        p.setFalli(falli);
        p.setOrarioFine(LocalDateTime.now().plusMinutes(10));
        return p;
    }

    public PartitaFreccette simulateFreccette() {
        PartitaFreccette p = new PartitaFreccette();
        p.setOrarioInizio(LocalDateTime.now());
        
        p.setModalita("501");
        p.setPunteggio(random.nextInt(300) + 100);
        p.setMosse(20 + random.nextInt(20));
        p.setNumero180(random.nextInt(3));
        p.setPercentualeBullseye(10.0 + (random.nextDouble() * 20.0));
        p.setGiocatoreVincitore(random.nextBoolean() ? "Giocatore 1" : "Giocatore 2");
        p.setOrarioFine(LocalDateTime.now().plusMinutes(8));
        return p;
    }
}
