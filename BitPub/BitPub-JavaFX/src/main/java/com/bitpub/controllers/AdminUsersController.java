package com.bitpub.controllers;

import com.bitpub.models.Utente;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller responsabile della gestione dell'interfaccia di amministrazione degli utenti.
 * L'architettura implementata segue rigorosamente il paradigma del client passivo guidato 
 * dall'ipermedia (HATEOAS), demandando la risoluzione degli endpoint al backend. 
 * Questo approccio garantisce una forte resilienza ai cambiamenti delle rotte lato server,
 * isolando la logica di presentazione dalla topologia della rete.
 *
 * @author Stefano Bellan 20054330
 */
public class AdminUsersController {

    // Componenti di input e visualizzazione per la ricerca e la consultazione della base utenti
    @FXML private TextField searchField;
    @FXML private TableView<Utente> usersTable;
    @FXML private TableColumn<Utente, String> colUsername, colEmail, colRole, colStato;
    @FXML private TableColumn<Utente, Double> colCredito;
    
    // Controlli operativi per l'alterazione dello stato e dei privilegi delle entità selezionate
    @FXML private Button toggleRoleButton;
    @FXML private Button toggleStatusButton;

    // Struttura dati reattiva legata bidirezionalmente alla TableView
    private final ObservableList<Utente> masterData = FXCollections.observableArrayList();
    
    // Client REST singleton per l'orchestrazione delle chiamate HTTP asincrone
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Entry-point del ciclo di vita del controller JavaFX.
     * Si occupa di mappare le proprietà del modello di dominio sulle colonne visive,
     * configurare la reattività dei controlli in base al contesto di selezione e 
     * avviare il popolamento iniziale della griglia dati.
     */
    @FXML
    public void initialize() {
        // Mappatura riflettiva dei campi della classe Utente sulle rispettive colonne
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colCredito.setCellValueFactory(new PropertyValueFactory<>("credito"));
        colStato.setCellValueFactory(new PropertyValueFactory<>("stato"));

        // Associazione della lista osservabile alla tabella per aggiornamenti automatici del DOM
        usersTable.setItems(masterData);

        // Listener reattivo sul modello di selezione della tabella per gestire dinamicamente lo stato dei bottoni
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean isSelected = (newSelection != null);
            toggleRoleButton.setDisable(!isSelected);
            if (toggleStatusButton != null) toggleStatusButton.setDisable(!isSelected);
        });

        // Trigger del fetch iniziale per presentare la lista utenti all'apertura della vista
        handleSearch();
    }

    /**
     * Intercetta la richiesta di ricerca testuale e innesca il recupero dei dati.
     * Il processo interroga preliminarmente la Root API per localizzare la collezione utenti,
     * compone dinamicamente la query string in conformità agli standard REST e delega
     * la mutazione della UI al thread grafico una volta processato il payload HATEOAS.
     */
    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim();

        // Fase di discovery ipermediale partendo dal punto di ingresso noto dell'API
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
            .thenCompose(root -> {
                String usersUrl = root.getLinkSafe("users");
                
                // Sanitizzazione e accodamento del parametro di ricerca se l'utente ha inserito un filtro
                if (!query.isEmpty()) {
                    usersUrl += "?search=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
                }
                
                // Avvio della richiesta effettiva verso la risorsa identificata
                return restClient.getAsync(usersUrl, JsonObject.class);
            })
            .thenAccept(response -> {
                // Estrapolazione delle entità dal wrapper JSON specifico del framework server
                List<Utente> listaEstratta = extractUsersFromHateoas(response);

                // Sincronizzazione sicura del data binding sul JavaFX Application Thread
                Platform.runLater(() -> masterData.setAll(listaEstratta));
            })
            .exceptionally(ex -> {
                // Intercettazione degli errori di rete per informare l'amministratore senza causare crash
                Platform.runLater(() -> mostraAlert("Errore", "Impossibile recuperare gli utenti: " + ex.getMessage(), Alert.AlertType.ERROR));
                return null;
            });
    }

    /**
     * Gestisce la logica di elevazione o revoca dei privilegi per l'utente evidenziato.
     * Implementa difese a livello di client per prevenire autolesionismo sui profili di amministrazione
     * e si affida alla presenza dei link operativi (HATEOAS) per convalidare l'autorizzazione all'azione.
     */
    @FXML
    public void handleToggleRole() {
        Utente selezionato = usersTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) return;

        // Blocco di sicurezza lato client per impedire modifiche accidentali alla struttura di amministrazione
        if ("ADMIN".equals(selezionato.getRole())) {
            mostraAlert("Azione Negata", "Non è possibile modificare il ruolo di un Amministratore.", Alert.AlertType.WARNING);
            return;
        }

        // Verifica della reale disponibilità dell'operazione valutando i link forniti dal server
        if (selezionato.getLinks() == null || !selezionato.getLinks().containsKey("toggle-role")) {
            mostraAlert("Errore", "L'operazione 'Cambio Ruolo' non è permessa per questo utente.", Alert.AlertType.ERROR);
            return;
        }

        String toggleUrl = selezionato.getLinkHref("toggle-role");

        // Richiesta di conferma esplicita per prevenire operazioni distruttive non intenzionali
        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION, "Vuoi cambiare il ruolo di " + selezionato.getUsername() + "?");
        conferma.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                // Esecuzione del cambio di stato tramite chiamata idempotente
                restClient.putAsync(toggleUrl, null, JsonObject.class)
                    .thenAccept(res -> {
                        Platform.runLater(() -> {
                            mostraAlert("Successo", "Ruolo aggiornato correttamente.", Alert.AlertType.INFORMATION);
                            // Ricarica la vista per garantire coerenza con il nuovo stato del database
                            handleSearch(); 
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> mostraAlert("Errore", "Modifica fallita: " + ex.getMessage(), Alert.AlertType.ERROR));
                        return null;
                    });
            }
        });
    }

    /**
     * Inverte lo stato di attivazione (Attivo/Sospeso) dell'utente selezionato.
     * Invia una richiesta PUT asincrona al server per persistere la modifica.
     */
    @FXML
    public void handleToggleStatus() {
        Utente selezionato = usersTable.getSelectionModel().getSelectedItem();
        if (selezionato == null) return;

        // URL-encoding dello username per gestire spazi e caratteri speciali nel path, usando un URLEncoder con UTF-8
        String usernameEncoded = URLEncoder.encode(selezionato.getUsername(), StandardCharsets.UTF_8).replace("+", "%20");
        String endpoint = restClient.getRootUrl().replace("/home", "") + "/users/" + usernameEncoded + "/toggle-status";

        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION, "Vuoi cambiare lo stato di " + selezionato.getUsername() + "?");
        conferma.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                restClient.putAsync(endpoint, null, JsonObject.class)
                    .thenAccept(res -> {
                        Platform.runLater(() -> {
                            mostraAlert("Successo", "Stato aggiornato correttamente.", Alert.AlertType.INFORMATION);
                            handleSearch(); 
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> mostraAlert("Errore", "Modifica fallita: " + ex.getMessage(), Alert.AlertType.ERROR));
                        return null;
                    });
            }
        });
    }

    /**
     * Isolatore architetturale per la deserializzazione di payload complessi.
     * Analizza l'albero JSON per supportare sia lo standard HAL (Hypertext Application Language)
     * utilizzato da Spring Data REST, sia strutture di paginazione classiche, 
     * rendendo il client agnostico rispetto all'impacchettamento del server.
     *
     * @param response L'oggetto JSON grezzo ricevuto dal backend
     * @return Una lista tipizzata di istanze Utente pronte per il binding
     */
    private List<Utente> extractUsersFromHateoas(JsonObject response) {
        List<Utente> utenti = new ArrayList<>();
        try {
            // Risoluzione della struttura HAL standard basata sull'oggetto _embedded
            if (response.has("_embedded")) {
                JsonObject embedded = response.getAsJsonObject("_embedded");
                // Identificazione dinamica della chiave che wrappa l'array di risorse
                String key = embedded.keySet().iterator().next();
                JsonArray array = embedded.getAsJsonArray(key);
                for (JsonElement el : array) {
                    utenti.add(restClient.getGson().fromJson(el, Utente.class));
                }
            } 
            // Fallback per endpoint che implementano la paginazione Spring standard
            else if (response.has("content")) {
                JsonArray array = response.getAsJsonArray("content");
                for (JsonElement el : array) {
                    utenti.add(restClient.getGson().fromJson(el, Utente.class));
                }
            }
        } catch (Exception e) {
            System.err.println("Errore parsing HATEOAS utenti: " + e.getMessage());
        }
        return utenti;
    }

    /**
     * Metodo di utilità per centralizzare e standardizzare la creazione delle finestre di dialogo,
     * riducendo la duplicazione del codice di gestione dell'interfaccia utente.
     *
     * @param titolo Il testo da mostrare nella barra del titolo della finestra
     * @param testo Il contenuto testuale descrittivo del messaggio
     * @param tipo La gravità dell'avviso che influenza l'iconografia mostrata
     */
    private void mostraAlert(String titolo, String testo, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(testo);
        alert.showAndWait();
    }
}