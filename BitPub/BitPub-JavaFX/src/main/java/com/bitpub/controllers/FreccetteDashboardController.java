package com.bitpub.controllers;

import com.bitpub.models.PartitaFreccette;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import java.util.List;

public class FreccetteDashboardController {

    // Componenti della tua interfaccia grafica (collegati tramite FXML)
    public TableView<PartitaFreccette> tabellaPartite;
    public Button bottoneAggiorna;
    public Label statoLabel;

    private final RestClient restClient = new RestClient();

    /**
     * Metodo chiamato, ad esempio, all'apertura della schermata.
     */
    public void caricaDatiDalCloud() {
        // Mostriamo all'utente che stiamo caricando (siamo ancora nel thread della UI qui)
        statoLabel.setText("Caricamento in corso...");

        // Usiamo il RestClient creato nella Fase 15
        restClient.faiChiamataGet("/partite/freccette", RispostaHateoas.class)
                .thenAccept(risposta -> {

                    // --- SIAMO NEL THREAD ASINCRONO (DI RETE) ---
                    // Qui NON possiamo toccare l'interfaccia grafica!

                    // 1. Stefano: Estrazione dei dati e dei link HATEOAS
                    List<PartitaFreccette> partiteRicevute = (List<PartitaFreccette>) risposta.getData();
                    String linkAggiornamento = risposta.getLinks().get("update").getHref();

                    // 2. Timothy: Passaggio sicuro al Thread Grafico di JavaFX
                    // Usiamo Platform.runLater per "spedire" le modifiche all'interfaccia
                    Platform.runLater(() -> {

                        // --- SIAMO TORNATI NEL THREAD DI JAVAFX (SICURO) ---
                        // Luca approverà: tutte le modifiche visive sono qui dentro!

                        // Aggiorniamo la tabella con i nuovi dati
                        tabellaPartite.getItems().setAll(partiteRicevute);
                        statoLabel.setText("Dati caricati con successo!");

                        // Assegnazione dinamica dell'azione al bottone usando il link HATEOAS (Compito di Stefano)
                        bottoneAggiorna.setOnAction(evento -> {
                            System.out.println("L'utente ha cliccato! Chiamo il link dinamico: " + linkAggiornamento);
                            // Qui potrai fare una nuova chiamata HTTP usando 'linkAggiornamento'
                        });
                    });

                })
                .exceptionally(errore -> {
                    // Gestione degli errori (es. server spento)
                    // Anche i messaggi di errore devono andare nel runLater se modificano la UI!
                    Platform.runLater(() -> {
                        statoLabel.setText("Errore di connessione: " + errore.getMessage());
                    });
                    return null;
                });
    }
}