package com.bitpub.buffer;

import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Architettura di buffering concorrente ad alte prestazioni per il nodo Edge.
 * Subentra alla precedente implementazione deprecata (MessageBuffer) per fornire
 * un'infrastruttura robusta a supporto del pattern Store-and-Forward.
 * Sfrutta le primitive di blocco native della piattaforma Java (LinkedBlockingQueue)
 * per disaccoppiare i thread produttori (sensori hardware) dai thread consumatori (uplink Cloud),
 * azzerando la necessità di lock manuali o blocchi synchronized e garantendo la tenuta
 * del sistema anche a fronte di massicci picchi di carico (spike).
 *
 * @author Stefano Bellan 20054330
 */
public class BufferDatiEdge {

    // Registratore eventi di sistema agganciato all'infrastruttura SLF4J
    private static final Logger logger = LoggerFactory.getLogger(BufferDatiEdge.class);

    // Coda FIFO thread-safe delegata all'isolamento della memoria condivisa
    private final LinkedBlockingQueue<String> queue;

    /**
     * Inizializza il layer di persistenza volatile in memoria.
     * Impone un limite capacitivo rigido per implementare nativamente logiche di backpressure
     * e tutelare l'heap della Virtual Machine da scenari di Out Of Memory (OOM)
     * qualora il Cloud risultasse irraggiungibile per periodi prolungati.
     *
     * @param capacity Il tetto massimo di elementi allocabili prima dell'arresto dei produttori
     */
    public BufferDatiEdge(int capacity) {
        // L'allocazione parametrica previene l'espansione indefinita della coda in RAM
        this.queue = new LinkedBlockingQueue<>(capacity);
        logger.info("[BUFFER] BufferDatiEdge inizializzato con capacità massima: {}", capacity);
    }

    /**
     * Accoda un nuovo evento telemetrico originato dai layer di intercettazione locale.
     * Implementa una strategia di accodamento puramente bloccante: qualora la coda risulti
     * satura, il thread chiamante viene ibernato a livello di kernel in attesa di spazio,
     * frenando fisicamente l'acquisizione dei dati in ingresso.
     *
     * @param log L'involucro testuale JSON rappresentante il payload applicativo
     * @throws InterruptedException Propagata agli strati superiori se il thread subisce un preempt o un kill
     */
    public void put(String log) throws InterruptedException {
        // L'impiego di put() anziché offer() assicura che nessun dato venga mai droppato per capienza esaurita
        queue.put(log);
        logger.debug("[BUFFER] Evento accodato. Dimensione attuale: {}", queue.size());
    }

    /**
     * Estrae l'evento più remoto disponibile (approccio FIFO) destinato all'esportazione verso il Cloud.
     * La chiamata sospende operativamente il thread consumatore (stato WAITING) in caso
     * di buffer vuoto, ottimizzando l'efficienza energetica e azzerando il consumo CPU
     * altrimenti generato da cicli di active-polling.
     *
     * @return Il tracciato JSON prelevato dalla testa della coda
     * @throws InterruptedException Sollevata nel caso il sistema ordini lo smantellamento del demone di esportazione
     */
    public String take() throws InterruptedException {
        // Il blocco intrinseco garantisce che l'InoltroCloudTask lavori solo su dati concreti
        return queue.take();
    }

    /**
     * Ispeziona la saturazione attuale della struttura dati.
     * Si rivela un metodo essenziale per l'esposizione di metriche infrastrutturali
     * e la costruzione di dashboard diagnostiche lato server.
     *
     * @return Il volume istantaneo dei pacchetti trattenuti in memoria
     */
    public int size() {
        return queue.size();
    }
}