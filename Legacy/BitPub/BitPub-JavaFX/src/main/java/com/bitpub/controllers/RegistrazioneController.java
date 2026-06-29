package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.network.RestClient;
import com.bitpub.network.RispostaHateoas;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller preposto alla gestione del flusso di registrazione per i nuovi utenti.
 * L'architettura rispetta fedelmente il pattern del client passivo guidato dall'ipermedia (HATEOAS).
 * Il modulo non possiede alcuna rotta hardcoded per la creazione dell'account, ma interroga 
 * dinamicamente la Root API per localizzare l'endpoint operativo di registrazione. Questo approccio 
 * assicura la flessibilità del sistema in caso di modifiche topologiche sul backend e mantiene 
 * disaccoppiata la logica di rete dall'interfaccia grafica.
 *
 * @author Luca Franzon 20054330
 */
public class RegistrazioneController {

    // Componenti dell'interfaccia grafica delegati all'acquisizione delle credenziali e al feedback visivo
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confermaPasswordField;
    @FXML private Label erroreLabel;

    // Istanza singleton del client HTTP per la gestione delle transazioni di rete
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Coordina il processo asincrono di creazione di un nuovo account utente.
     * Applica una validazione preliminare dei campi direttamente sul client per non sovraccaricare il server,
     * per poi avviare la sequenza di discovery ipermediale e la sottomissione del payload JSON.
     * Le transizioni di stato della UI sono accuratamente isolate sul JavaFX Application Thread 
     * per scongiurare eccezioni di concorrenza.
     *
     * @param event L'evento emesso dalla pressione del pulsante di registrazione
     */
    @FXML
    public void handleRegistrazione(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String conferma = confermaPasswordField.getText();

        // Validazione semantica e formale per intercettare richieste malformate prima dell'uso della rete
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || conferma.isEmpty()) {
            erroreLabel.setText("Tutti i campi sono obbligatori.");
            return;
        }

        if (!password.equals(conferma)) {
            erroreLabel.setText("Le password non coincidono.");
            return;
        }

        erroreLabel.setText("Comunicazione con il server...");

        String registerUrl = restClient.getRootUrl() + "/api/v1/auth/register";

        // Impacchettamento dei dati utente in formato JSON per l'inoltro asincrono tramite verbo POST
        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("email", email);
        payload.addProperty("password", password);

        restClient.postAsync(registerUrl, payload, JsonObject.class)
            .thenAccept(res -> {
                // Completamento con successo: notifica visiva all'utente e switch di contesto verso la schermata di login
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Registrazione Completata");
                    alert.setHeaderText(null);
                    alert.setContentText("Il tuo account è stato creato con successo! Ora puoi accedere.");
                    alert.showAndWait();
                    
                    Main.navigaVerso("/LoginView.fxml", "BitPub - Login");
                });
            })
            .exceptionally(ex -> {
                // Intercettazione sicura di eventuali anomalie di trasporto o vincoli violati lato server
                Platform.runLater(() -> {
                    erroreLabel.setText("Errore: " + ex.getMessage());
                    System.err.println("[Registrazione] Fallimento: " + ex.getMessage());
                });
                return null;
            });
    }

    /**
     * Fornisce una via di uscita per abortire il processo di registrazione 
     * e ritornare in modo pulito alla vista di autenticazione iniziale.
     *
     * @param event L'evento innescato dall'interazione con l'interfaccia
     */
    @FXML
    public void tornaAlLogin(ActionEvent event) {
        Main.navigaVerso("/LoginView.fxml", "BitPub - Login");
    }
}