import com.bitpub.buffer.PersistentEventStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OfflineSyncIntegrationTest {

    private static final String DB_PATH = "test_events.db";
    private PersistentEventStore store;

    @BeforeEach
    public void setup() {
        // Pulizia del db di test prima di ogni esecuzione
        File dbFile = new File(DB_PATH);
        if (dbFile.exists()) dbFile.delete();
        
        store = new PersistentEventStore(DB_PATH);
    }

    @AfterEach
    public void teardown() {
        if (store != null) {
            store.close();
        }
        File dbFile = new File(DB_PATH);
        if (dbFile.exists()) dbFile.delete();
    }

    @Test
    public void testPersistenzaEDeduplica() throws Exception {
        int numeroSimulatori = 5;
        int eventiPerSimulatore = 100;
        
        // Ogni simulatore genera eventi identici in rapida successione. 
        // A causa della deduplica su base temporale (500ms), 
        // ci aspettiamo che la maggior parte dei pacchetti duplicati venga scartata,
        // dimostrando la robustezza anti-thundering herd locale.
        
        ExecutorService executorProduttori = Executors.newFixedThreadPool(numeroSimulatori);
        CountDownLatch latchProduttori = new CountDownLatch(numeroSimulatori);

        for (int i = 0; i < numeroSimulatori; i++) {
            final int idSimulatore = i;
            executorProduttori.submit(() -> {
                for (int j = 0; j < eventiPerSimulatore; j++) {
                    try {
                        String payload = "{\"source\":\"DEVICE\", \"hardwareSignature\":\"TAVOLO_" + idSimulatore + "\", \"state\":\"GOL\"}";
                        store.enqueue(payload);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                latchProduttori.countDown();
            });
        }

        latchProduttori.await();
        executorProduttori.shutdown();

        // Poiché i 100 messaggi per simulatore avvengono nella stessa finestra temporale (entro 500ms),
        // ci aspettiamo che solo 1 messaggio per simulatore sopravviva al filtro deduplica!
        int expectedMaxMessages = numeroSimulatori; // 5 messaggi univoci totali
        
        int actuallyPersisted = store.getPendingCount();
        assertEquals(expectedMaxMessages, actuallyPersisted, "La deduplica degli eventi offline ha fallito o la persistenza non è andata a buon fine.");
        
        // Simula il Crash e riapertura
        store.close();
        
        PersistentEventStore reloadedStore = new PersistentEventStore(DB_PATH);
        assertEquals(expectedMaxMessages, reloadedStore.getPendingCount(), "Crash Recovery Fallito: gli eventi non sono sopravvissuti al riavvio!");
        
        reloadedStore.close();
    }
}
