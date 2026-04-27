import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class BiliardoApiClient {

    // Inizializziamo il Logger per questa specifica classe
    private static final Logger logger = LoggerFactory.getLogger(BiliardoApiClient.class);

    private final HttpClient httpClient;

    public BiliardoApiClient() {
        // Creazione del client HTTP
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        logger.info("BiliardoApiClient inizializzato con successo.");
    }

    /**
     * Esempio di metodo per richiedere i dati di una partita di Biliardo al Cloud.
     */
    public void recuperaDatiPartita(String endpointId) {
        String url = "http://localhost:8080/api/biliardo/partite/" + endpointId;

        // 1. LOG: Segnaliamo l'avvio della chiamata HTTP
        logger.info("Avvio chiamata HTTP GET verso l'endpoint: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.bitpub.v1+json") // Esempio di Semantic Versioning
                .GET()
                .build();

        // Invio asincrono: questo viene eseguito in un thread in background
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // 2. LOG: Gestione della risposta HTTP
                    if (response.statusCode() == 200) {
                        logger.info("Risposta ricevuta con successo (Status 200). Elaborazione dati...");
                        // logger.debug("Payload JSON ricevuto: {}", response.body()); // Utile se vuoi vedere il body solo in debug

                        // Passaggio cruciale: aggiorniamo la UI usando Platform.runLater
                        Platform.runLater(() -> {
                            logger.info("Aggiornamento dell'interfaccia JavaFX in corso...");
                            // Qui inserirai il codice per aggiornare le tabelle/grafica
                        });
                    } else {
                        // Segnaliamo un problema non bloccante (es. 404 Not Found)
                        logger.warn("Ricevuto status code inatteso: {}. La risorsa potrebbe non esistere.", response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    // 3. LOG: Gestione degli errori (es. server irraggiungibile)
                    logger.error("Errore critico durante la chiamata HTTP verso {}. Causa: {}", url, ex.getMessage(), ex);

                    Platform.runLater(() -> {
                        logger.error("Mostro la finestra di errore all'utente nella UI.");
                        // Qui puoi chiamare un metodo che mostra un Alert grafico all'utente
                    });
                    return null;
                });
    }
}