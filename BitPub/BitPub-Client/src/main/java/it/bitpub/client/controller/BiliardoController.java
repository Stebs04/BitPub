import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;

public class BiliardoController {

    @FXML private TableView<Biliardo> tabellaBiliardi;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    /**
     * Esempio: Carica i biliardi usando un link ricevuto in precedenza
     * @param urlProvenienteDaHateoas L'URL estratto dai link di una risposta precedente
     */
    public void caricaBiliardi(String urlProvenienteDaHateoas) {
        // Creazione della richiesta asincrona
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlProvenienteDaHateoas))
                .header("Accept", "application/json; v=1.0.0") // Semantic Versioning [cite: 80]
                .GET()
                .build();

        // Invio asincrono per non bloccare la UI
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(json -> {
                    // Deserializzazione con GSON
                    Biliardo[] lista = gson.fromJson(json, Biliardo[].class);

                    // IMPORTANTE: Aggiornamento della UI solo nel thread grafico
                    Platform.runLater(() -> {
                        tabellaBiliardi.getItems().setAll(lista);
                        System.out.println("Dati caricati correttamente via HATEOAS!");
                    });
                })
                .exceptionally(ex -> {
                    System.err.println("Errore durante la chiamata: " + ex.getMessage());
                    return null;
                });
    }
}