package com.bitpub.controllers;

import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.time.LocalTime;
import java.util.Map;

/**
 * Controller per la gestione integrata del Calciobalilla con integrazione API Cloud.
 * <p>
 * Funge da orchestratore tra l'interfaccia utente JavaFX, le chiamate asincrone alle API Cloud
 * e l'interazione simulata con i sensori fisici. Il controller si affida al modulo unificato
 * {@link RestClient} per la comunicazione di rete, garantendo una fluida user experience.
 * </p>
 *
 * @author Stefano Bellan
 * @version 1.1
 * @since 1.0
 */
public class CalciobalillaGestioneController {

    @FXML private Label lblStatoApi;
    @FXML private Label lblPunteggioRosso;
    @FXML private Label lblPunteggioBlu;
    @FXML private TextArea txtLogEventi;
    @FXML private ListView<String> listaDati;

    // L'utilizzo dell'istanza unificata centralizza la configurazione degli header (es. JWT e versioning)
    // ed evita di disperdere la logica HTTP cruda nei controller UI.
    private final RestClient restClient = new RestClient();

    /**
     * Metodo di inizializzazione chiamato automaticamente dal framework JavaFX.
     * Bootstrap dello stato dell'interfaccia e pre-fetch dei dati dal database distribuito.
     */
    @FXML
    public void initialize() {
        logEvento("Interfaccia di gestione avviata e pronta.");
        sincronizzaDatiCloud();
    }

    /**
     * Gestisce la creazione di un nuovo "Locale" tramite richiesta POST al backend.
     * * @implNote Sfrutta una Map per la definizione del payload; il RestClient si occuperà
     * della serializzazione in JSON tramite la libreria Gson, isolando la UI da dettagli implementativi REST.
     */
    @FXML
    public void gestisciCreaLocale() {
        impostaStatoCaricamento("Creazione Locale in corso...", "#b45309", "#fde047");

        Map<String, String> payloadLocale = Map.of(
                "nome", "BitPub Centrale",
                "citta", "Milano"
        );

        restClient.faiChiamataPost("/locali", payloadLocale, String.class)
                .thenAccept(risposta -> {
                    // Protezione della UI: il thread HTTP worker non può manipolare direttamente lo Scene Graph.
                    // Si usa runLater per accodare l'aggiornamento visivo al thread principale.
                    Platform.runLater(() -> {
                        logEvento("✅ API: Nuovo Locale creato con successo nel Cloud.");
                        ripristinaStatoApi();
                        sincronizzaDatiCloud(); 
                    });
                })
                .exceptionally(errore -> {
                    // Protezione della notifica di errore sul JavaFX Application Thread
                    Platform.runLater(() -> gestisciErroreApi("Errore creazione Locale", errore));
                    return null;
                });
    }

    /**
     * Gestisce la creazione di un nuovo "Torneo" per il Calciobalilla tramite API Cloud.
     */
    @FXML
    public void gestisciCreaTorneo() {
        impostaStatoCaricamento("Creazione Torneo in corso...", "#4c1d95", "#c4b5fd");

        Map<String, String> payloadTorneo = Map.of(
                "nome", "Torneo Estivo BitPub",
                "premio", "100 Euro"
        );

        restClient.faiChiamataPost("/tornei", payloadTorneo, String.class)
                .thenAccept(risposta -> {
                    // Transizione di stato visiva protetta per rispetto delle policy thread-safe di JavaFX
                    Platform.runLater(() -> {
                        logEvento("🏆 API: Nuovo Torneo registrato con successo.");
                        ripristinaStatoApi();
                        sincronizzaDatiCloud();
                    });
                })
                .exceptionally(errore -> {
                    // Prevenzione del crash dell'app in caso di API irraggiungibili o timeout
                    Platform.runLater(() -> gestisciErroreApi("Errore creazione Torneo", errore));
                    return null;
                });
    }

    /**
     * Innesca la modalità partita ed entra in ascolto degli eventi provenienti dal campo fisico.
     * * @implNote Per fini dimostrativi avvia un thread demone che emula la ricezione MQTT di un gol
     * dopo un ritardo predefinito.
     */
    @FXML
    public void gestisciAvviaPartita() {
        logEvento("⚽ Comando inviato: In attesa di eventi dal Simulatore...");

        lblPunteggioRosso.setText("0");
        lblPunteggioBlu.setText("0");

        // Generazione asincrona di eventi per simulare la latenza del protocollo MQTT e del sensore IoT
        new Thread(() -> {
            try {
                Thread.sleep(2500); // Simulazione latenza di gioco di 2.5 secondi
                
                // Iniettiamo la modifica del punteggio nel thread grafico. Nessun worker thread può toccare lblPunteggioRosso.
                Platform.runLater(() -> {
                    lblPunteggioRosso.setText("1");
                    logEvento("🔔 LIVE: GOL Squadra Rossa rilevato dal sensore!");
                });
            } catch (InterruptedException e) {
                // Il ripristino dell'interrupted status previene la perdita del segnale in cicli superiori
                Thread.currentThread().interrupt();
                logEvento("⚠️ Simulazione interrotta: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Esegue il pull dello storico partite dal database distribuito.
     * * @implNote Utilizza l'array mapping di Gson per deserializzare direttamente liste di risorse HATEOAS.
     */
    @FXML
    public void sincronizzaDatiCloud() {
        logEvento("🔄 Sincronizzazione con il Cloud in corso...");
        listaDati.getItems().clear();
        listaDati.getItems().add("Scaricamento dati in corso...");

        restClient.faiChiamataGet("/calciobalilla", RispostaHateoas[].class)
                .thenAccept(risposteArray -> {
                    // Renderizzazione dei dati HATEOAS protetta sul ciclo di ridisegno JavaFX
                    Platform.runLater(() -> {
                        listaDati.getItems().clear();
                        if (risposteArray != null && risposteArray.length > 0) {
                            listaDati.getItems().add("✅ Dati ricevuti dal server Cloud:");

                            for(int i = 0; i < risposteArray.length; i++) {
                                listaDati.getItems().add("Partita registrata #" + (i+1) + " - Dati HATEOAS connessi");
                            }
                            logEvento("✅ Sincronizzazione completata.");
                        } else {
                            listaDati.getItems().add("Nessuna partita presente al momento nel DB.");
                            logEvento("ℹ️ Sincronizzazione: Il database sembra vuoto.");
                        }
                    });
                })
                .exceptionally(errore -> {
                    // Fallback visivo sicuro in caso di disconnessione o eccezioni di rete
                    Platform.runLater(() -> {
                        listaDati.getItems().clear();
                        listaDati.getItems().add("⚠️ Errore di connessione al Cloud.");
                        gestisciErroreApi("Errore Sincronizzazione GET", errore);
                    });
                    return null;
                });
    }

    /**
     * Esegue il push di un messaggio formattato nella console virtuale della UI.
     *
     * @param messaggio Il contenuto informativo dell'evento di sistema da tracciare.
     */
    private void logEvento(String messaggio) {
        String time = LocalTime.now().withNano(0).toString(); // Troncamento ai secondi per leggibilità pulita
        txtLogEventi.appendText("[" + time + "] " + messaggio + "\n");
    }

    /**
     * Manipola il CSS inline dell'etichetta di stato superiore per comunicare l'attività in background.
     *
     * @param testo     Testo di feedback per l'utente.
     * @param bgColor   Codice colore esadecimale per lo sfondo.
     * @param textColor Codice colore esadecimale per il font.
     */
    private void impostaStatoCaricamento(String testo, String bgColor, String textColor) {
        lblStatoApi.setText(testo);
        lblStatoApi.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    /**
     * Ripristina la visualizzazione dell'indicatore visivo di "idle" delle comunicazioni di rete.
     */
    private void ripristinaStatoApi() {
        lblStatoApi.setText("API Pronta");
        lblStatoApi.setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    /**
     * Centralizza il rendering degli stati d'errore (500, connessione persa) informando
     * tempestivamente l'utente per facilitare il debug dell'infrastruttura Cloud.
     *
     * @param contesto Contesto locale o metodo in cui si è innescata la failure.
     * @param errore   Causa scatenante dell'eccezione propagata asincronamente.
     */
    private void gestisciErroreApi(String contesto, Throwable errore) {
        logEvento("❌ ERRORE " + contesto + ": " + errore.getMessage());
        lblStatoApi.setText("Rete Disconnessa");
        lblStatoApi.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 14px;");
        System.err.println(contesto + ": " + errore.getMessage());
    }
}