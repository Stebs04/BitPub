package com.bitpub.sync;

import com.bitpub.buffer.PersistentEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsabile per l'ispezione della persistenza locale durante la fase di Bootstrap.
 * Assicura che eventuali pacchetti orfani (un-acknowledged) interrotti 
 * da un precedente Crash di Sistema vengano identificati e ri-processati.
 */
public class ReplayManager {

    private static final Logger logger = LoggerFactory.getLogger(ReplayManager.class);

    private final PersistentEventStore eventStore;

    public ReplayManager(PersistentEventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Esegue l'ispezione della coda persistente.
     * La notifica ai worker avviene automaticamente all'interno di PersistentEventStore,
     * questo metodo stampa un report di stato.
     */
    public void recoverFromCrash() {
        int pending = eventStore.getPendingCount();
        if (pending > 0) {
            logger.warn("[REPLAY MANAGER] Rilevati {} eventi pendenti dal precedente ciclo di vita. Re-inserimento nella coda attiva completato.", pending);
        } else {
            logger.info("[REPLAY MANAGER] Nessun evento pendente rilevato. Avvio pulito.");
        }
    }
}
