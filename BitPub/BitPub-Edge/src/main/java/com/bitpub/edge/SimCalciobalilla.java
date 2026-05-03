package com.bitpub.edge;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

/**
 * Simulatore stocastico per la generazione di eventi del Calciobalilla 
 * direttamente sul nodo Edge. Invia gli eventi alla coda condivisa.
 */
public class SimCalciobalilla implements Runnable {

    private final Integer tableId;
    private final BlockingQueue<FoosballEvent> eventQueue;
    private final Random random;
    private final int MAX_GOALS = 10;
    
    private int scoreBlue = 0;
    private int scoreRed = 0;

    public SimCalciobalilla(Integer tableId, BlockingQueue<FoosballEvent> eventQueue) {
        this.tableId = tableId;
        this.eventQueue = eventQueue;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            // Evento iniziale
            eventQueue.put(new FoosballEvent(tableId, "START", scoreBlue, scoreRed, "IN_PROGRESS", null));

            while (!Thread.currentThread().isInterrupted() && scoreBlue < MAX_GOALS && scoreRed < MAX_GOALS) {
                // Attesa randomica tra 2 e 5 secondi per il prossimo goal
                Thread.sleep(2000 + random.nextInt(3000));

                if (random.nextBoolean()) {
                    scoreBlue++;
                } else {
                    scoreRed++;
                }

                String status = (scoreBlue >= MAX_GOALS || scoreRed >= MAX_GOALS) ? "FINISHED" : "IN_PROGRESS";
                String winner = null;
                if ("FINISHED".equals(status)) {
                    winner = scoreBlue >= MAX_GOALS ? "BLUE" : "RED";
                }

                eventQueue.put(new FoosballEvent(tableId, "GOAL", scoreBlue, scoreRed, status, winner));
            }
        } catch (InterruptedException e) {
            System.out.println("[SimCalciobalilla] Simulazione interrotta per il tavolo " + tableId);
            try {
                eventQueue.put(new FoosballEvent(tableId, "FORCE_STOPPED", scoreBlue, scoreRed, "FORCE_STOPPED", null));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
