package com.bitpub;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

/**
 * Simulatore software puro per le Freccette.
 * Genera un singolo step (lancio) di una partita ogni volta che viene eseguito.
 */
public class SimFreccette implements Runnable {

    private final String idLocale;
    private final String idDispositivo;
    private final Random random;

    // Ecco la nostra coda
    private final BlockingQueue<Object> codaEventi;

    // Lo stato del gioco viene salvato qui
    private int punteggio;

    // CORREZIONE 1: Aggiunto BlockingQueue<Object> codaEventi ai parametri!
    public SimFreccette(String idLocale, String idDispositivo, BlockingQueue<Object> codaEventi) {
        this.idLocale = idLocale;
        this.idDispositivo = idDispositivo;
        this.codaEventi = codaEventi; // Ora questo assegnamento funziona
        this.random = new Random();
        this.punteggio = 501;
    }

    @Override
    public void run() {
        // Se la partita precedente è finita, ne iniziamo una nuova in automatico
        if (punteggio <= 0) {
            punteggio = 501;
            System.out.println("\n[SimFreccette " + idDispositivo + "] --- NUOVA PARTITA 501 INIZIATA ---");
        }

        // Generiamo i punti del singolo lancio
        int puntiLancio = random.nextInt(61);
        String tipoEvento = "LANCIO_NORMALE";

        // Logica del gioco
        if (puntiLancio == 50) {
            tipoEvento = "BULLSEYE";
            punteggio -= puntiLancio;
        } else if (punteggio - puntiLancio < 0 || punteggio - puntiLancio == 1) {
            tipoEvento = "BUSTO";
            // In caso di busto il punteggio non scende
        } else {
            punteggio -= puntiLancio;
        }

        if (punteggio == 0) {
            tipoEvento = "VITTORIA";
        }

        // Creiamo l'oggetto Dati
        EventoFreccette evento = new EventoFreccette(tipoEvento, puntiLancio, punteggio);

        // CORREZIONE 2: Inseriamo 'evento' nella coda, non 'partitaCorrente'
        codaEventi.offer(evento);

        System.out.println("[SimFreccette " + idDispositivo + "] " + tipoEvento + "! Lancio: " + puntiLancio + " | Rimanenti: " + punteggio);
    }
}