import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class EdgeConcurrencyTest {

    @Test
    public void testNessunaRaceConditionConBlockingQueue() throws InterruptedException {
        // Questa è la coda che Stefano e Timothy useranno per comunicare
        LinkedBlockingQueue<String> codaEventi = new LinkedBlockingQueue<>();

        int numeroSimulatori = 10;
        int eventiPerSimulatore = 1000;
        int totaleEventiAttesi = numeroSimulatori * eventiPerSimulatore;

        // Usiamo AtomicInteger per contare in modo sicuro quanti eventi estrae il consumatore
        AtomicInteger eventiConsumati = new AtomicInteger(0);

        // Simuliamo i Thread Produttori (es. i vari tavoli da Biliardo, Calciobalilla, ecc.)
        ExecutorService executorProduttori = Executors.newFixedThreadPool(numeroSimulatori);
        CountDownLatch latchProduttori = new CountDownLatch(numeroSimulatori);

        for (int i = 0; i < numeroSimulatori; i++) {
            final int idSimulatore = i;
            executorProduttori.submit(() -> {
                for (int j = 0; j < eventiPerSimulatore; j++) {
                    try {
                        // Inserimento thread-safe: non serve nessun "synchronized"!
                        codaEventi.put("Evento " + j + " dal simulatore " + idSimulatore);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                latchProduttori.countDown(); // Segnala che questo simulatore ha finito
            });
        }

        // Simuliamo il Thread Consumatore (il task di Timothy che invia al Cloud)
        Thread consumatore = new Thread(() -> {
            try {
                // Continua a consumare finché non ha ricevuto tutti gli eventi previsti
                while (eventiConsumati.get() < totaleEventiAttesi) {
                    // take() mette in pausa il thread se la coda è vuota, senza consumare CPU
                    String evento = codaEventi.take();
                    eventiConsumati.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumatore.start();

        // Aspettiamo che tutti i produttori finiscano di inserire gli eventi
        latchProduttori.await();
        // Aspettiamo che il consumatore finisca di estrarli tutti
        consumatore.join();
        executorProduttori.shutdown();

        // Validazione finale: se i numeri combaciano, non c'è stata nessuna perdita di dati!
        assertEquals(totaleEventiAttesi, eventiConsumati.get(),
                "Errore: Il numero di eventi consumati non corrisponde a quelli prodotti!");
    }
}