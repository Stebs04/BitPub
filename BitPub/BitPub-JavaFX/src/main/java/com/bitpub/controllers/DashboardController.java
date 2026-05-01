package com.bitpub.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.bitpub.network.AsyncHttpService;
import com.bitpub.network.SessionManager; // <-- Aggiunto l'import per recuperare il Token
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;

/**
 * Controller per la Dashboard Generale con gestione logica dei ruoli.
 * Permette di visualizzare le diverse funzionalità in base al ruolo selezionato
 * (Admin, Gestore del Locale, Utente Base) instradando l'utente verso il modulo corretto.
 *
 * @author Stefano Bellan (Architettura Generale e Dashboard)
 * @author Luca Franzon (Integrazione Biliardo)
 * @author Timothy Giolito (Integrazione Freccette)
 */
public class DashboardController {

    // --- Costanti ---
    private static final String RUOLO_ADMIN = "Admin";
    private static final String RUOLO_GESTORE = "Gestore";
    private static final String RUOLO_UTENTE = "Utente Base";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // --- Componenti FXML ---
    @FXML private ComboBox<String> cmbRuoli;
    
    @FXML private VBox panelAdmin;
    @FXML private VBox panelGestore;
    @FXML private VBox panelUtente;

    @FXML private ListView<String> listAdminLocali;
    @FXML private ListView<String> listGestoreLocali;
    @FXML private ListView<String> listUtenteAttivita;

    private final AsyncHttpService httpService;
    private static final String BASE_URL = "http://localhost:8080";

    public DashboardController() {
        this.httpService = new AsyncHttpService();
    }

    /**
     * Inizializzazione automatica della Dashboard all'apertura.
     * Seleziona il primo ruolo di default e formatta la vista corrispondente.
     */
    @FXML
    public void initialize() {
        cmbRuoli.getSelectionModel().selectFirst();
        cambioRuolo();
    }

    /**
     * Gestisce la visibilità dei pannelli in base al ruolo selezionato nella ComboBox.
     * Modifica attivamente il layout nascondendo e disabilitando i nodi non pertinenti.
     */
    @FXML
    public void cambioRuolo() {
        String ruolo = cmbRuoli.getValue();
        
        // Modifica dei nodi strutturali UI effettuata in sicurezza sul JavaFX Thread.
        // L'utilizzo di setManaged(false) garantisce che il VBox nascosto non occupi spazio residuo.
        Platform.runLater(() -> {
            panelAdmin.setVisible(RUOLO_ADMIN.equals(ruolo));
            panelAdmin.setManaged(RUOLO_ADMIN.equals(ruolo));
            
            panelGestore.setVisible(RUOLO_GESTORE.equals(ruolo));
            panelGestore.setManaged(RUOLO_GESTORE.equals(ruolo));
            
            panelUtente.setVisible(RUOLO_UTENTE.equals(ruolo));
            panelUtente.setManaged(RUOLO_UTENTE.equals(ruolo));
        });
    }

    /**
     * Metodo Helper centralizzato per inserire log nelle liste grafiche.
     * Formatta il testo, appone un timestamp e inietta l'aggiornamento sul thread grafico.
     * * @param listView La lista di destinazione in cui inserire il log.
     * @param messaggio Il contenuto del log da mostrare.
     */
    private void aggiungiLog(ListView<String> listView, String messaggio) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        // Garantisce che anche se l'helper viene chiamato da un thread asincrono (es. dopo una response HTTP),
        // l'aggiunta dell'item non causi eccezioni grafiche.
        Platform.runLater(() -> listView.getItems().add(0, "[" + timestamp + "] " + messaggio));
    }

    /**
     * Metodo Helper per mostrare i risultati parsati in una comoda finestra Alert di JavaFX,
     * consentendo all'utente di ispezionare facilmente il JSON ricevuto senza intasare la ListView.
     */
    private void mostraAlertRisultato(String titolo, String jsonRisposta) {
        Platform.runLater(() -> {
            try {
                // Formatting the JSON beautifully with GSON
                JsonElement jsonElement = JsonParser.parseString(jsonRisposta);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String prettyJson = gson.toJson(jsonElement);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(titolo);
                alert.setHeaderText("Dettagli Partita Ricevuti");
                // Usiamo un font monospaced per renderizzare bene il JSON tramite CSS inline
                alert.getDialogPane().setStyle("-fx-font-family: 'Consolas', monospace;");
                alert.setContentText(prettyJson);
                alert.showAndWait();
            } catch (Exception e) {
                // Gestione caso in cui il risultato non sia JSON (es. un errore testo semplice o un booleano)
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(titolo);
                alert.setHeaderText("Risultato Operazione");
                alert.setContentText(jsonRisposta);
                alert.showAndWait();
            }
        });
    }

    // ==========================================
    //            AZIONI AMMINISTRATORE
    // ==========================================

    @FXML
    public void visualizzaTuttiLocali() {
        aggiungiLog(listAdminLocali, "(Admin) Richiesta elenco globale Locali in corso...");
    }

    @FXML
    public void creaNuovoLocale() {
        aggiungiLog(listAdminLocali, "(Admin) Apertura form creazione nuovo Locale...");
    }

    // ==========================================
    //              AZIONI GESTORE
    // ==========================================

    @FXML
    public void visualizzaLocaliGestore() {
        aggiungiLog(listGestoreLocali, "(Gestore) Recupero propri locali e macchine attive.");
    }

    @FXML
    public void creaTorneoGestore() {
        aggiungiLog(listGestoreLocali, "(Gestore) Creazione nuovo Torneo in corso...");
    }

    // ==========================================
    //              AZIONI UTENTE BASE
    // ==========================================

    @FXML
    public void giocaCalciobalilla() {
        aggiungiLog(listUtenteAttivita, "(Utente) Richiesta Calciobalilla in corso...");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/calciobalilla/stats"))
                // FIX: Aggiunto l'header Authorization con il JWT Token!
                .header("Authorization", "Bearer " + SessionManager.getInstance().getJwtToken())
                .GET()
                .build();
        httpService.sendAsync(request, 
            HttpResponse::body,
            res -> {
                aggiungiLog(listUtenteAttivita, "(Utente) Calciobalilla: Dati ricevuti con successo!");
                mostraAlertRisultato("Statistiche Calciobalilla", res);
            },
            err -> {
                aggiungiLog(listUtenteAttivita, "(Utente) Errore Calciobalilla: " + err.getMessage());
                mostraAlertRisultato("Errore Calciobalilla", err.getMessage());
            }
        );
    }

    @FXML
    public void giocaFreccette() {
        aggiungiLog(listUtenteAttivita, "(Utente) Richiesta Freccette in corso...");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/statistiche/freccette")) // Assicurati che l'URL coincida con il backend
                // FIX: Aggiunto l'header Authorization con il JWT Token!
                .header("Authorization", "Bearer " + SessionManager.getInstance().getJwtToken())
                .GET()
                .build();
        httpService.sendAsync(request, 
            HttpResponse::body,
            res -> {
                aggiungiLog(listUtenteAttivita, "(Utente) Freccette: Partita recuperata con successo!");
                mostraAlertRisultato("Partita Freccette", res);
            },
            err -> {
                aggiungiLog(listUtenteAttivita, "(Utente) Errore Freccette: " + err.getMessage());
                mostraAlertRisultato("Errore Freccette", err.getMessage());
            }
        );
    }

    @FXML
    public void giocaBiliardo() {
        aggiungiLog(listUtenteAttivita, "(Utente) Richiesta Biliardo in corso...");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/biliardo/statistiche")) // Assicurati che l'URL coincida con il backend
                // FIX: Aggiunto l'header Authorization con il JWT Token!
                .header("Authorization", "Bearer " + SessionManager.getInstance().getJwtToken())
                .GET()
                .build();
        httpService.sendAsync(request, 
            HttpResponse::body,
            res -> {
                aggiungiLog(listUtenteAttivita, "(Utente) Biliardo: Evento recuperato con successo!");
                mostraAlertRisultato("Evento Biliardo", res);
            },
            err -> {
                aggiungiLog(listUtenteAttivita, "(Utente) Errore Biliardo: " + err.getMessage());
                mostraAlertRisultato("Errore Biliardo", err.getMessage());
            }
        );
    }

    @FXML
    public void iscrivitiTorneo() {
        aggiungiLog(listUtenteAttivita, "(Utente) Ricerca tornei aperti per iscrizione...");
    }
}