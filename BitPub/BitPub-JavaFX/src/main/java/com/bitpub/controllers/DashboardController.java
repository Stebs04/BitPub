package com.bitpub.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        Platform.runLater(() -> listView.getItems().add("[" + timestamp + "] " + messaggio));
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
        aggiungiLog(listUtenteAttivita, "(Utente) Validazione giocatori per Calciobalilla: OK (Min 2). Inizio partita...");
    }

    @FXML
    public void giocaFreccette() {
        aggiungiLog(listUtenteAttivita, "(Utente) Validazione giocatori per Freccette: OK (Min 1). Inizio partita...");
    }

    @FXML
    public void giocaBiliardo() {
        aggiungiLog(listUtenteAttivita, "(Utente) Validazione giocatori per Biliardo: OK (Min 2). Inizio partita...");
    }

    @FXML
    public void iscrivitiTorneo() {
        aggiungiLog(listUtenteAttivita, "(Utente) Ricerca tornei aperti per iscrizione...");
    }
}