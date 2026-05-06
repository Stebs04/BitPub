package com.bitpub.edge;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

/**
 * Simulatore stocastico per il Calciobalilla sull'Edge Node.
 *
 * <p>Genera tre tipi di eventi:</p>
 * <ul>
 *   <li><b>GOAL BLU (35%)</b> – incrementa scoreBlue</li>
 *   <li><b>GOAL ROSSO (35%)</b> – incrementa scoreRed</li>
 *   <li><b>FALLO / RULLATA (30%)</b> – incrementa totaleRullate senza modificare punteggi</li>
 * </ul>
 *
 * <p>Calcola la {@code durataMediaPallinaSecondi} come media dei tempi
 * trascorsi tra un evento e il successivo (approssimazione del "tempo in gioco" della pallina).</p>
 */
public class SimCalciobalilla implements Runnable {

    private final Integer tableId;
    private final Long    sessionId;
    private final BlockingQueue<FoosballEvent> eventQueue;
    private final Random  random;

    private static final int MAX_GOALS = 10;

    // ── Stato partita ─────────────────────────────────────────────────────────
    private int  scoreBlue     = 0;
    private int  scoreRed      = 0;
    private int  totaleRullate = 0;

    // ── Calcolo durataMediaPallinaSecondi ─────────────────────────────────────
    private long totalPlayTimeMs = 0;
    private int  totalEvents     = 0;
    private long lastEventTime;

    public SimCalciobalilla(Integer tableId, Long sessionId, BlockingQueue<FoosballEvent> eventQueue) {
        this.tableId   = tableId;
        this.sessionId = sessionId;
        this.eventQueue = eventQueue;
        this.random    = new Random();
    }

    @Override
    public void run() {
        System.out.println("[SimCalciobalilla] Avvio — tableId=" + tableId + " sessionId=" + sessionId);
        lastEventTime = System.currentTimeMillis();

        try {
            // Evento iniziale
            // eventQueue.put(buildEvent("START", "IN_PROGRESS", null)); // Muted Simulator

            while (!Thread.currentThread().isInterrupted()
                    && scoreBlue < MAX_GOALS
                    && scoreRed  < MAX_GOALS) {

                // Attesa randomica tra 2 e 5 secondi
                long sleepMs = 2000 + random.nextInt(3000);
                Thread.sleep(sleepMs);

                // ── Calcolo durata media ──────────────────────────────────
                long now = System.currentTimeMillis();
                totalPlayTimeMs += (now - lastEventTime);
                totalEvents++;
                lastEventTime = now;

                // ── Generazione evento (probabilità) ─────────────────────
                int rand = random.nextInt(100);

                if (rand < 35) {
                    // ── GOAL BLU ──────────────────────────────────────────
                    scoreBlue++;
                    String status = scoreBlue >= MAX_GOALS ? "FINISHED" : "IN_PROGRESS";
                    String winner = scoreBlue >= MAX_GOALS ? "BLUE" : null;
                    // eventQueue.put(buildEvent("GOAL", status, winner)); // Muted Simulator
                    System.out.println("[SimCalciobalilla] GOAL BLU → " + scoreBlue + "-" + scoreRed + " [" + status + "]");

                } else if (rand < 70) {
                    // ── GOAL ROSSO ────────────────────────────────────────
                    scoreRed++;
                    String status = scoreRed >= MAX_GOALS ? "FINISHED" : "IN_PROGRESS";
                    String winner = scoreRed >= MAX_GOALS ? "RED" : null;
                    // eventQueue.put(buildEvent("GOAL", status, winner)); // Muted Simulator
                    System.out.println("[SimCalciobalilla] GOAL ROSSO → " + scoreBlue + "-" + scoreRed + " [" + status + "]");

                } else {
                    // ── FALLO / RULLATA ───────────────────────────────────
                    totaleRullate++;
                    // eventQueue.put(buildEvent("FALLO", "IN_PROGRESS", null)); // Muted Simulator
                    System.out.println("[SimCalciobalilla] FALLO/RULLATA → rullate totali:" + totaleRullate);
                }
            }

        } catch (InterruptedException e) {
            System.out.println("[SimCalciobalilla] Simulazione interrotta (tavolo " + tableId + ")");
            try {
                // eventQueue.put(buildEvent("FORCE_STOPPED", "FORCE_STOPPED", null)); // Muted Simulator
            } catch (Exception ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Costruisce un {@link FoosballEvent} con lo stato corrente della partita.
     * Calcola automaticamente la durata media della pallina in secondi.
     */
    private FoosballEvent buildEvent(String eventType, String status, String winner) {
        int durataMedia = totalEvents > 0
                ? (int) (totalPlayTimeMs / totalEvents / 1000L)
                : 0;
        return new FoosballEvent(
                tableId, sessionId, eventType,
                scoreBlue, scoreRed, status, winner,
                totaleRullate, durataMedia
        );
    }
}
