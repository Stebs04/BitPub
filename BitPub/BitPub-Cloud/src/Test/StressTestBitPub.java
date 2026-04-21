import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class StressTestBitPub {

    // Parametri dello stress test: modificali come preferisci!
    private static final int NUMERO_RICHIESTE = 150; // Quante richieste sparare in totale
    private static final int NUMERO_THREAD = 50;     // Quanti thread (operai) lavorano in parallelo
    private static final String URL_SERVER = "http://localhost:8080/api/v1/eventi";

    public static void main(String[] args) {
        System.out.println("=== INIZIO STRESS TEST BITPUB ===");
        System.out.println("Bersaglio: " + URL_SERVER);
        System.out.println("Sparando " + NUMERO_RICHIESTE + " richieste simultanee...");

        // Creiamo il nostro "capocantiere" con una squadra di 50 operai
        ExecutorService esecutore = Executors.newFixedThreadPool(NUMERO_THREAD);

        // Creiamo un singolo client HTTP che tutti gli operai useranno
        HttpClient client = HttpClient.newHttpClient();

        // Contatori sicuri per il multithreading
        AtomicInteger successi = new AtomicInteger(0);
        AtomicInteger fallimenti = new AtomicInteger(0);

        // Prepariamo il ciclo per inviare le richieste
        for (int i = 0; i < NUMERO_RICHIESTE; i++) {
            final int numeroRichiesta = i + 1; // Usiamo una variabile finale per la lambda

            // Diamo un compito a un operaio
            esecutore.submit(() -> {
                try {
                    // Costruiamo la richiesta (uguale a quella che usa il tuo JavaFX)
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(URL_SERVER))
                            .header("Accept", "application/vnd.bitpub.v1+json")
                            .GET()
                            .build();

                    // Inviamo la richiesta in modo SINCRONO (l'operaio aspetta la risposta)
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // Controlliamo se è andata a buon fine
                    if (response.statusCode() == 200) {
                        successi.incrementAndGet(); // Aggiungiamo 1 ai successi
                        System.out.println("[OK] Richiesta " + numeroRichiesta + " completata.");
                    } else {
                        fallimenti.incrementAndGet(); // Aggiungiamo 1 ai fallimenti
                        System.out.println("[ERRORE] Richiesta " + numeroRichiesta + " fallita con codice: " + response.statusCode());
                    }

                } catch (Exception e) {
                    fallimenti.incrementAndGet();
                    System.out.println("[CRASH] Richiesta " + numeroRichiesta + " fallita a causa di: " + e.getMessage());
                }
            });
        }

        // Diciamo al capocantiere di non accettare più nuovi compiti
        esecutore.shutdown();

        try {
            // Aspettiamo che tutti gli operai finiscano il lavoro (massimo 1 minuto di attesa)
            if (!esecutore.awaitTermination(1, TimeUnit.MINUTES)) {
                esecutore.shutdownNow();
            }
        } catch (InterruptedException e) {
            esecutore.shutdownNow();
        }

        // Stampiamo i risultati finali per il Capitolo 6!
        System.out.println("\n=== RISULTATI STRESS TEST ===");
        System.out.println("Richieste totali inviate: " + NUMERO_RICHIESTE);
        System.out.println("Successi (HTTP 200): " + successi.get());
        System.out.println("Fallimenti/Errori: " + fallimenti.get());
        System.out.println("Se i successi sono pari al totale, il database e i Lock hanno retto perfettamente!");
    }
}