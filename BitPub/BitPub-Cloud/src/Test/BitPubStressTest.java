import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BitPubStressTest {

    // --- PARAMETRI DI CONFIGURAZIONE ---
    // Cambia questo URL con l'endpoint reale della vostra API REST Spring
    private static final String TARGET_URL = "http://localhost:8080/api/v1/locali";
    // Numero di richieste totali da inviare
    private static final int TOTAL_REQUESTS = 500;
    // Numero di thread che lavorano in contemporanea (le "connessioni simultanee")
    private static final int CONCURRENT_THREADS = 100;

    public static void main(String[] args) {
        System.out.println("Avvio dello Stress Test di BitPub...");
        System.out.println("URL Target: " + TARGET_URL);
        System.out.println("Richieste totali: " + TOTAL_REQUESTS);
        System.out.println("Thread concorrenti: " + CONCURRENT_THREADS);
        System.out.println("--------------------------------------------------");

        // Creiamo il client HTTP
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)) // Timeout per non bloccare il test all'infinito
                .build();

        // Creiamo un gruppo di thread
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);

        // Variabili sicure per il multithreading (AtomicInteger previene problemi di concorrenza)
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Una lista sincronizzata per salvare i tempi di ogni singola richiesta
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        // Prepariamo la lista dei "lavori" (le richieste HTTP) che i thread dovranno eseguire
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            tasks.add(() -> {
                Instant start = Instant.now(); // Inizio misurazione tempo
                try {
                    // Costruiamo la richiesta GET con l'header per il Semantic Versioning / HATEOAS
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(TARGET_URL))
                            .header("Accept", "application/json")
                            .GET()
                            .build();

                    // Invio della richiesta
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    Instant end = Instant.now(); // Fine misurazione tempo
                    long latency = Duration.between(start, end).toMillis(); // Calcolo latenza in millisecondi
                    latencies.add(latency);

                    // Controlliamo se la risposta HTTP è un successo (codice 200)
                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
                return null;
            });
        }

        try {
            // Misuriamo il tempo totale del test per calcolare il throughput
            Instant testStart = Instant.now();

            // Eseguiamo tutte le richieste in parallelo!
            executor.invokeAll(tasks);

            Instant testEnd = Instant.now();
            long totalTimeMillis = Duration.between(testStart, testEnd).toMillis();

            // Calcolo delle statistiche finali
            stampaRisultati(successCount.get(), errorCount.get(), totalTimeMillis, latencies);

        } catch (InterruptedException e) {
            System.err.println("Il test è stato interrotto!");
        } finally {
            // Spegniamo il gestore dei thread
            executor.shutdown();
        }
    }

    /**
     * Metodo helper per calcolare e stampare le statistiche finali
     */
    private static void stampaRisultati(int success, int errors, long totalTimeMillis, List<Long> latencies) {
        System.out.println("--------------------------------------------------");
        System.out.println("TEST COMPLETATO!");
        System.out.println("Richieste con successo (200 OK): " + success);
        System.out.println("Richieste fallite o in errore: " + errors);

        // Calcolo Throughput (Richieste gestite al secondo)
        double totalTimeSeconds = totalTimeMillis / 1000.0;
        double throughput = (success + errors) / totalTimeSeconds;
        System.out.printf("Tempo totale di esecuzione: %.2f secondi\n", totalTimeSeconds);
        System.out.printf("Throughput: %.2f richieste/secondo\n", throughput);

        // Calcolo Latenza (Media, Min, Max)
        if (!latencies.isEmpty()) {
            long max = latencies.stream().mapToLong(v -> v).max().orElse(0);
            long min = latencies.stream().mapToLong(v -> v).min().orElse(0);
            double avg = latencies.stream().mapToLong(v -> v).average().orElse(0);

            System.out.println("Latenza Minima: " + min + " ms");
            System.out.println("Latenza Massima: " + max + " ms");
            System.out.printf("Latenza Media: %.2f ms\n", avg);
        }
        System.out.println("--------------------------------------------------");
    }
}