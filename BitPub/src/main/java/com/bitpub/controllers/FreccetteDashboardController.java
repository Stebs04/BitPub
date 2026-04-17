package com.bitpub.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class FreccetteDashboardController {

    // L'annotazione @FXML collega queste variabili agli elementi con "fx:id" nel file XML
    @FXML private Label lblGiocatore1;
    @FXML private Label lblPunteggio1;
    @FXML private ProgressBar progressGiocatore1;

    @FXML private Label lblGiocatore2;
    @FXML private Label lblPunteggio2;
    @FXML private ProgressBar progressGiocatore2;

    // Impostiamo il punteggio iniziale tipico delle freccette
    private final int PUNTEGGIO_INIZIALE = 501;

    /**
     * Metodo chiamato automaticamente da JavaFX quando carica la schermata.
     */
    @FXML
    public void initialize() {
        // Qui potremmo fare eventuali setup iniziali
        System.out.println("Dashboard Freccette Inizializzata!");
    }

    /**
     * Metodo da chiamare quando riceviamo un nuovo evento/punteggio dal server.
     * * @param giocatore Il numero del giocatore (1 o 2)
     * @param punteggioRimanente Il punteggio attuale da mostrare
     */
    public void aggiornaPunteggio(int giocatore, int punteggioRimanente) {
        // Calcoliamo la percentuale della barra (es. 250 / 501 = ~0.5)
        double percentualeProgress = (double) punteggioRimanente / PUNTEGGIO_INIZIALE;

        // CRITICO: Deleghiamo l'aggiornamento visivo al thread grafico di JavaFX!
        Platform.runLater(() -> {
            if (giocatore == 1) {
                lblPunteggio1.setText(String.valueOf(punteggioRimanente));
                progressGiocatore1.setProgress(percentualeProgress);
            } else if (giocatore == 2) {
                lblPunteggio2.setText(String.valueOf(punteggioRimanente));
                progressGiocatore2.setProgress(percentualeProgress);
            }
        });
    }
}