package it.unibo.bitpub.javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.application.Platform;

public class BiliardoUIController {

    // L'annotazione @FXML collega questa variabile all'elemento nel file FXML
    @FXML
    private TextArea eventiBiliardoArea;

    // Questo metodo viene chiamato quando premi il pulsante "Aggiorna Dati Biliardo"
    @FXML
    public void aggiornaDati() {
        // Simuliamo l'arrivo di dati dal server Cloud in background
        new Thread(() -> {
            String nuovoEvento = "Evento: Palla 8 in buca! (Ricevuto da API REST)\n";

            // REGOLA FONDAMENTALE: Aggiorniamo la UI solo tramite Platform.runLater
            Platform.runLater(() -> {
                eventiBiliardoArea.appendText(nuovoEvento);
            });
        }).start();
    }
}