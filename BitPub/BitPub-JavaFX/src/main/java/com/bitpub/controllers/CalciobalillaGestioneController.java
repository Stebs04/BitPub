package com.bitpub.controllers;

import com.bitpub.network.AsyncHttpService;
import com.bitpub.network.HttpResponseParser;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.LocalTime;

/**
 * Controller per la gestione integrata del Calciobalilla con integrazione API Reale.
 * <p>
 * Funge da ponte tra l'interfaccia utente, le API Cloud (per la creazione di risorse come Locali e Tornei)
 * e i simulatori locali. Utilizza AsyncHttpService per chiamate non bloccanti garantendo
 * che l'interfaccia grafica rimanga sempre fluida e reattiva.
 * </p>
 *
 * @author Stefano Bellan
 */
public class CalciobalillaGestioneController {

    @FXML private Label lblStatoApi;
    @FXML private Label lblPunteggioRosso;
    @FXML private Label lblPunteggioBlu;
    @FXML private TextArea txtLogEventi;
    @FXML private ListView<String> listaDati;

    // Istanza del servizio asincrono per gestire le chiamate HTTP
    private final AsyncHttpService asyncHttpService = new AsyncHttpService();
    // Base URL delle tue API (assicurati che corrisponda alla porta del tuo Spring Boot)
    private final String BASE_URL = "http://localhost:8080/api";

    /**
     * Metodo di inizializzazione chiamato automaticamente da JavaFX al caricamento della vista.
     * Prepara l'interfaccia e avvia il primo download dei dati.
     */
    @FXML
    public void initialize() {
        logEvento("Interfaccia di gestione avviata e pronta.");
        sincronizzaDatiCloud();
    }

    /**
     * Gestisce l'azione di creazione di un nuovo "Locale" inviando una richiesta POST alle API Cloud.
     * I dati inseriti nel payload JSON andranno a popolare il tuo database.
     */
    @FXML
    public void gestisciCreaLocale() {
        impostaStatoCaricamento("Creazione Locale in corso...", "#b45309", "#fde047");

        // Payload JSON di esempio (potrai espanderlo legandolo a delle vere caselle di testo nell'interfaccia)
        String jsonPayload = "{ \"nome\": \"BitPub Centrale\", \"citta\": \"Milano\" }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/locali"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/resources.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Chiamata asincrona: passiamo la richiesta, come decodificarla e cosa fare in caso di successo o errore
        asyncHttpService.sendAsync(
                request,
                response -> response.body(), // Per una POST base, leggiamo semplicemente la stringa di ritorno
                risposta -> Platform.runLater(() -> {
                    logEvento("✅ API: Nuovo Locale creato con successo nel Cloud.");
                    ripristinaStatoApi();
                    sincronizzaDatiCloud(); // Aggiorniamo la vista generale dopo la creazione
                }),
                errore -> Platform.runLater(() -> gestisciErroreApi("Errore creazione Locale", errore))
        );
    }

    /**
     * Gestisce l'azione di creazione di un nuovo "Torneo" inviando una richiesta POST alle API Cloud.
     */
    @FXML
    public void gestisciCreaTorneo() {
        impostaStatoCaricamento("Creazione Torneo in corso...", "#4c1d95", "#c4b5fd");

        // Payload JSON di esempio per il Torneo
        String jsonPayload = "{ \"nome\": \"Torneo Estivo BitPub\", \"premio\": \"100 Euro\" }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tornei"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/resources.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        asyncHttpService.sendAsync(
                request,
                response -> response.body(),
                risposta -> Platform.runLater(() -> {
                    logEvento("🏆 API: Nuovo Torneo registrato con successo.");
                    ripristinaStatoApi();
                    sincronizzaDatiCloud();
                }),
                errore -> Platform.runLater(() -> gestisciErroreApi("Errore creazione Torneo", errore))
        );
    }

    /**
     * Avvia il monitoraggio live della partita.
     * In un'integrazione completa, questo metodo si collegherà al client MQTT
     * per ricevere i gol veri dal simulatore o dai sensori.
     */
    @FXML
    public void gestisciAvviaPartita() {
        logEvento("⚽ Comando inviato: In attesa di eventi dal Simulatore...");

        lblPunteggioRosso.setText("0");
        lblPunteggioBlu.setText("0");

        // Qui ho lasciato una piccola simulazione temporale per mostrarti come
        // aggiornare il punteggio quando riceverai un messaggio MQTT!
        new Thread(() -> {
            try {
                Thread.sleep(2500);
                Platform.runLater(() -> {
                    lblPunteggioRosso.setText("1");
                    logEvento("🔔 LIVE: GOL Squadra Rossa rilevato dal sensore!");
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Richiede l'aggiornamento dei dati storici dal Cloud tramite una GET HTTP.
     * Sfrutta l'architettura HATEOAS per popolare la lista destra dell'interfaccia.
     */
    @FXML
    public void sincronizzaDatiCloud() {
        logEvento("🔄 Sincronizzazione con il Cloud in corso...");
        listaDati.getItems().clear();
        listaDati.getItems().add("Scaricamento dati in corso...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/calciobalilla"))
                .header("Accept", "application/resources.v1+json")
                .GET()
                .build();

        asyncHttpService.sendAsync(
                request,
                response -> HttpResponseParser.parseJsonList(response, RispostaHateoas[].class),
                risposteLista -> Platform.runLater(() -> {
                    listaDati.getItems().clear();
                    if (risposteLista != null && !risposteLista.isEmpty()) {
                        listaDati.getItems().add("✅ Dati ricevuti dal server Cloud:");

                        // Scorre i dati ricevuti e popola la visualizzazione
                        for(int i = 0; i < risposteLista.size(); i++) {
                            listaDati.getItems().add("Partita registrata #" + (i+1) + " - Dati HATEOAS connessi");
                        }
                        logEvento("✅ Sincronizzazione completata.");
                    } else {
                        listaDati.getItems().add("Nessuna partita presente al momento nel DB.");
                        logEvento("ℹ️ Sincronizzazione: Il database sembra vuoto.");
                    }
                }),
                errore -> Platform.runLater(() -> {
                    listaDati.getItems().clear();
                    listaDati.getItems().add("⚠️ Errore di connessione al Cloud.");
                    gestisciErroreApi("Errore Sincronizzazione GET", errore);
                })
        );
    }

    /**
     * Aggiunge un messaggio di testo con l'orario attuale nell'area nera (console) dello schermo.
     *
     * @param messaggio Il testo da loggare a schermo.
     */
    private void logEvento(String messaggio) {
        String time = LocalTime.now().withNano(0).toString(); // Formato HH:mm:ss
        txtLogEventi.appendText("[" + time + "] " + messaggio + "\n");
    }

    /**
     * Cambia i colori dell'etichetta in alto a destra per mostrare all'utente che l'app sta lavorando.
     *
     * @param testo     Il messaggio da mostrare.
     * @param bgColor   Il colore di sfondo in formato HEX.
     * @param textColor Il colore del testo in formato HEX.
     */
    private void impostaStatoCaricamento(String testo, String bgColor, String textColor) {
        lblStatoApi.setText(testo);
        lblStatoApi.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    /**
     * Ripristina l'etichetta verde "API Pronta".
     */
    private void ripristinaStatoApi() {
        lblStatoApi.setText("API Pronta");
        lblStatoApi.setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    /**
     * Centralizza la gestione degli errori di rete: mostra l'errore sia nella console grafica
     * sia in un'etichetta rossa evidente, in modo che tu capisca subito se il server Spring Boot è spento.
     * * @param contesto Una breve descrizione di dove è avvenuto l'errore (es. "Errore creazione Locale").
     * @param errore   L'eccezione lanciata dalla chiamata di rete.
     */
    private void gestisciErroreApi(String contesto, Throwable errore) {
        logEvento("❌ ERRORE " + contesto + ": " + errore.getMessage());
        lblStatoApi.setText("Rete Disconnessa");
        lblStatoApi.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 14px;");
        System.err.println(contesto + ": " + errore.getMessage());
    }
}