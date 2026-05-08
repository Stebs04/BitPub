package com.bitpub;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

/**
 * Simulatore software puro per il Calciobalilla.
 * Esegue un singolo evento di gioco (gol o fallo) a ogni chiamata.
 */
public class SimCalciobalilla implements Runnable {

    private final String idLocale;
    private final String idDispositivo;
    private final Random random;
    private final BlockingQueue<Object> codaEventi;

    private PartitaCalciobalilla partitaCorrente;
    private final int MAX_GOL = 10;

    public SimCalciobalilla(String idLocale, String idDispositivo) {
        this.idLocale = idLocale;
        this.idDispositivo = idDispositivo;
        this.codaEventi = codaEventi;
        this.random = new Random();
        iniziaNuovaPartita();
    }

    private void iniziaNuovaPartita() {
        // Inizializza un nuovo oggetto Partita azzerando i punteggi
        partitaCorrente = new PartitaCalciobalilla(0, 0, 0, 0, 0);
        partitaCorrente.setOrarioInizio(LocalDateTime.now());
        System.out.println("\n[SimCalciobalilla " + idDispositivo + "] --- NUOVA PARTITA INIZIATA ---");
    }

    @Override
    public void run() {
        // Se qualcuno ha già vinto, resettiamo il tavolo per la prossima chiamata
        if (partitaCorrente.getGoalRossi() >= MAX_GOL || partitaCorrente.getGoalBlu() >= MAX_GOL) {
            iniziaNuovaPartita();
            return;
        }

        // Estraiamo casualmente l'evento che si è verificato sul tavolo
        int probabilita = random.nextInt(100);

        if (probabilita < 40) {
            partitaCorrente.setGoalRossi(partitaCorrente.getGoalRossi() + 1);
            partitaCorrente.setTotaleGol(partitaCorrente.getTotaleGol() + 1);
            System.out.println("[SimCalciobalilla] GOAL ROSSI! (" + partitaCorrente.getGoalRossi() + " - " + partitaCorrente.getGoalBlu() + ")");
        } else if (probabilita < 80) {
            partitaCorrente.setGoalBlu(partitaCorrente.getGoalBlu() + 1);
            partitaCorrente.setTotaleGol(partitaCorrente.getTotaleGol() + 1);
            System.out.println("[SimCalciobalilla] GOAL BLU! (" + partitaCorrente.getGoalRossi() + " - " + partitaCorrente.getGoalBlu() + ")");
        } else {
            partitaCorrente.setTotaleRullate(partitaCorrente.getTotaleRullate() + 1);
            System.out.println("[SimCalciobalilla] FALLO! Rullata registrata.");
        }

        // Simuliamo un tempo medio in cui la pallina è rimasta in gioco
        partitaCorrente.setDurataMediaPallinaSecondi(10 + random.nextInt(20));

        // Controlliamo se con questo evento la partita si è conclusa
        if (partitaCorrente.getGoalRossi() >= MAX_GOL || partitaCorrente.getGoalBlu() >= MAX_GOL) {
            partitaCorrente.setOrarioFine(LocalDateTime.now());
            String vincitore = partitaCorrente.getGoalRossi() == MAX_GOL ? "ROSSI" : "BLU";
            System.out.println("[SimCalciobalilla] PARTITA TERMINATA! Vittoria " + vincitore);
        }

        codaEventi.offer(partitaCorrente);
    }
}