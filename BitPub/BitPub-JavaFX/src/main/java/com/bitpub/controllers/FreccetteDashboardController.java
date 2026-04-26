package com.bitpub.controllers;

import com.bitpub.models.PartitaFreccette;
import com.bitpub.models.StatisticheFreccette;
import com.bitpub.network.RispostaHateoas;
import com.bitpub.network.RestClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import java.util.List;

/**
 * Controller per la Dashboard Freccette - Progetto BitPub.
 * Implementa le logiche di networking asincrono, gestione link HATEOAS
 * e visualizzazione statistiche aggregate.
 * Aggiornato per la Fase 23: Sincronizzazione sistematica JavaFX Thread.
 *
 * @author Timothy Giolito
 */
public class FreccetteDashboardController {

    // Componenti dell'interfaccia collegati tramite FXML
    @FXML public TableView<PartitaFreccette> tabellaPartite;
    @FXML public Button bottoneAggiorna;
    @FXML public Label statoLabel;

    // --- FASE 17: Logica UI Freccette (Timothy) ---
    @FXML public Label labelTotale180;
    @FXML public Label labelMediaPunti;

    private final RestClient restClient = new RestClient();

    /**
     * Metodo di inizializzazione di JavaFX.
     * Carica automaticamente i dati all'apertura della schermata.
     */
    @FXML
    public void initialize() {
        caricaDatiDalCloud();
        caricaStatisticheFreccette();
    }

    /**
     * FASI 15 & 16: Networking e HATEOAS.
     * FASE 23: Platform.runLater per OGNI interazione UI.
     */
    public void caricaDatiDalCloud() {
        // Timothy: Garantiamo che anche l'impostazione dello stato iniziale sia sul thread FX
        Platform.runLater(() -> statoLabel.setText("Caricamento in corso..."));

        restClient.faiChiamataGet("/partite/freccette", RispostaHateoas.class)
                .thenAccept(risposta -> {
                    // Stefano: Estrazione dati e link HATEOAS (nel thread HTTP)
                    List<PartitaFreccette> partiteRicevute = (List<PartitaFreccette>) risposta.getData();

                    String linkUpdate = "";
                    if (risposta.getLinks() != null && risposta.getLinks().containsKey("update")) {
                        RispostaHateoas.LinkDettaglio link = (RispostaHateoas.LinkDettaglio) risposta.getLinks().get("update");
                        if (link != null) {
                            linkUpdate = link.getHref();
                        }
                    }

                    final String finalLink = linkUpdate;

                    // Timothy: Aggiornamento UI sicuro tramite Platform.runLater
                    Platform.runLater(() -> {
                        tabellaPartite.getItems().setAll(partiteRicevute);
                        statoLabel.setText("Dati aggiornati con successo.");

                        // Assegnazione dinamica dell'azione (Stefano)
                        if (!finalLink.isEmpty()) {
                            bottoneAggiorna.setOnAction(evento -> {
                                System.out.println("Eseguo aggiornamento su: " + finalLink);
                                // Logica per chiamate successive...
                            });
                        }
                    });
                })
                .exceptionally(errore -> {
                    Platform.runLater(() -> statoLabel.setText("Errore di rete: " + errore.getMessage()));
                    return null;
                });
    }

    /**
     * FASE 17: Aggregazione Dati (Timothy).
     * FASE 23: Messa in sicurezza totale del thread UI.
     */
    public void caricaStatisticheFreccette() {
        // Timothy: Messa in sicurezza anche per i testi temporanei
        Platform.runLater(() -> {
            labelTotale180.setText("...");
            labelMediaPunti.setText("...");
        });

        restClient.faiChiamataGet("/statistiche/freccette", StatisticheFreccette.class)
                .thenAccept(statistiche -> {
                    // Timothy: Riversamento risultati dal thread di rete al thread grafico
                    Platform.runLater(() -> {
                        // Visualizzazione del numero totale di 180
                        labelTotale180.setText(String.valueOf(statistiche.getTotale180()));

                        // Visualizzazione media punti formattata a due decimali
                        labelMediaPunti.setText(String.format("%.2f", statistiche.getMediaPuntiTorneo()));
                    });
                })
                .exceptionally(errore -> {
                    // Luca: Controllo integrità per evitare crash in caso di errore
                    Platform.runLater(() -> {
                        labelTotale180.setText("N/D");
                        labelMediaPunti.setText("N/D");
                    });
                    return null;
                });
    }
}