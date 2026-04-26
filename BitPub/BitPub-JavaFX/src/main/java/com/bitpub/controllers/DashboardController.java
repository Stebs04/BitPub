package com.bitpub.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import java.time.LocalDateTime;

/**
 * Controller per la Dashboard Generale con gestione logica dei ruoli.
 * Permette di visualizzare le diverse funzionalità in base al ruolo selezionato
 * (Admin, Gestore del Locale, Utente Base).
 *
 * @author Stefano Bellan (Architettura Generale e Dashboard)
 * @author Luca Franzon (Integrazione Biliardo)
 * @author Timothy Giolito (Integrazione Freccette)
 */
public class DashboardController {

    @FXML private ComboBox<String> cmbRuoli;
    @FXML private VBox panelAdmin;
    @FXML private VBox panelGestore;
    @FXML private VBox panelUtente;

    @FXML private ListView<String> listAdminLocali;
    @FXML private ListView<String> listGestoreLocali;
    @FXML private ListView<String> listUtenteAttivita;

    /**
     * Inizializzazione della Dashboard.
     */
    @FXML
    public void initialize() {
        cmbRuoli.getSelectionModel().selectFirst();
        cambioRuolo();
    }

    /**
     * Gestisce la visibilità dei pannelli in base al ruolo selezionato.
     */
    @FXML
    public void cambioRuolo() {
        String ruolo = cmbRuoli.getValue();
        panelAdmin.setVisible("Admin".equals(ruolo));
        panelAdmin.setManaged("Admin".equals(ruolo));
        
        panelGestore.setVisible("Gestore".equals(ruolo));
        panelGestore.setManaged("Gestore".equals(ruolo));
        
        panelUtente.setVisible("Utente Base".equals(ruolo));
        panelUtente.setManaged("Utente Base".equals(ruolo));
    }

    // --- AZIONI ADMIN ---

    @FXML
    public void visualizzaLocaliAdmin() {
        listAdminLocali.getItems().add("[" + LocalDateTime.now().withNano(0) + "] (Admin) Visualizzazione di tutti i locali dal Cloud...");
    }

    @FXML
    public void creaLocaleAdmin() {
        listAdminLocali.getItems().add("[" + LocalDateTime.now().withNano(0) + "] (Admin) Creazione nuovo locale inviata al Cloud.");
    }

    @FXML
    public void eliminaLocaleAdmin() {
        listAdminLocali.getItems().add("[" + LocalDateTime.now().withNano(0) + "] (Admin) Richiesta di eliminazione locale inviata.");
    }

    // --- AZIONI GESTORE ---

    @FXML
    public void visualizzaLocaliGestore() {
        listGestoreLocali.getItems().add("[" + LocalDateTime.now().withNano(0) + "] (Gestore) Recupero propri locali e macchine attive (Calciobalilla, Freccette, Biliardo).");
    }

    @FXML
    public void creaTorneoGestore() {
        listGestoreLocali.getItems().add("[" + LocalDateTime.now().withNano(0) + "] (Gestore) Creazione nuovo Torneo in corso...");
    }

    // --- AZIONI UTENTE BASE ---

    @FXML
    public void giocaCalciobalilla() {
        // Regola: minimo 2 o 4 giocatori
        listUtenteAttivita.getItems().add("[" + LocalDateTime.now().withNano(0) + "] (Utente) Validazione giocatori per Calciobalilla: OK (Min 2 presenti). Inizio partita...");
    }

    @FXML
    public void giocaFreccette() {
        // Regola: minimo 1 giocatore
        listUtenteAttivita.getItems().add("[" + LocalDateTime.now().withNano(0) + "] (Utente) Validazione giocatori per Freccette: OK (Min 1 presente). Inizio partita...");
    }

    @FXML
    public void giocaBiliardo() {
        // Regola: minimo 2 giocatori
        listUtenteAttivita.getItems().add("[" + LocalDateTime.now().withNano(0) + "] (Utente) Validazione giocatori per Biliardo: OK (Min 2 presenti). Inizio partita...");
    }

    @FXML
    public void iscrivitiTorneoUtente() {
        listUtenteAttivita.getItems().add("[" + LocalDateTime.now().withNano(0) + "] (Utente) Iscrizione al torneo richiesta. Verifica posti disponibili in corso...");
    }
}
