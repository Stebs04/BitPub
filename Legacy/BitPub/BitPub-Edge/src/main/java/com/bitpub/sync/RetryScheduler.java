package com.bitpub.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calcola i tempi di attesa per i fallimenti di rete implementando 
 * un Exponential Backoff con Jitter per mitigare il problema del Thundering Herd
 * quando la connessione viene ristabilita.
 */
public class RetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RetryScheduler.class);
    
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double multiplier;
    
    private int attempt = 0;

    public RetryScheduler(long initialDelayMs, long maxDelayMs, double multiplier) {
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.multiplier = multiplier;
    }

    /**
     * Sospende il thread corrente per il tempo calcolato (Backoff bloccante)
     */
    public void sleepWithBackoff() throws InterruptedException {
        long delay = computeDelay();
        logger.debug("[RETRY SCHEDULER] Sospensione per {} ms (Tentativo n. {})", delay, attempt);
        Thread.sleep(delay);
        attempt++;
    }
    
    /**
     * Resetta il contatore di tentativi (es. a seguito di un invio andato a buon fine)
     */
    public void reset() {
        if (attempt > 0) {
            logger.debug("[RETRY SCHEDULER] Backoff resettato.");
        }
        attempt = 0;
    }

    private long computeDelay() {
        double exponentialDelay = initialDelayMs * Math.pow(multiplier, attempt);
        // Jitter +/- 10%
        double jitter = exponentialDelay * 0.1 * (Math.random() * 2 - 1);
        long delay = (long) (exponentialDelay + jitter);
        return Math.min(delay, maxDelayMs);
    }
}
