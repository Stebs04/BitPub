package it.unibo.bitpub.javafx.controller;

import it.unibo.bitpub.javafx.network.BiliardoApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import java.net.http.HttpRequest;
import java.net.URI;

public class BiliardoUIController {

    // Riferimenti agli elementi grafici (definiti nel file FXML)
    @FXML
    private Label serieMassimaLabel;
    @FXML
    private ListView<String> storicoListView;

    private BiliardoApiClient apiClient;

    // Metodo chiamato in automatico da JavaFX quando la schermata viene caricata
    @FXML
    public void initialize() {
        this.apiClient = new BiliardoApiClient();
    }

    // Metodo collegato a un pulsante "Carica Statistiche" nella UI
    @FXML
    public void caricaDatiDashboard() {
        // Luca, qui simuliamo la creazione della richiesta.
        // Ricorda che l'header 'Accept' sarà gestito da Timothy!
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/v1/biliardo/statistiche"))
                .GET()
                .build();

        // 1. Facciamo la chiamata asincrona
        apiClient.getStatisticheBiliardo(request)
                // 2. thenAccept dice cosa fare quando i dati sono pronti
                .thenAccept(statistiche -> {

                    // 3. REGOLA FONDAMENTALE: Aggiorniamo la grafica tramite il thread di JavaFX
                    Platform.runLater(() -> {
                        // Popoliamo la Label con il numero massimo
                        serieMassimaLabel.setText(String.valueOf(statistiche.getSerieMassimaPalle()));

                        // Svuotiamo e riempiamo la lista dello storico
                        storicoListView.getItems().clear();
                        storicoListView.getItems().addAll(statistiche.getStoricoPartite());
                    });

                })
                .exceptionally(errore -> {
                    // Gestione di un eventuale errore di rete (es. Cloud Server spento)
                    System.err.println("Errore di connessione: " + errore.getMessage());
                    return null;
                });
    }
}