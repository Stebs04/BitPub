package com.bitpub.controllers;

import com.bitpub.Main;
import com.bitpub.models.AuthRequest;
import com.bitpub.models.AuthResponse;
import com.bitpub.network.RestClient;
import com.bitpub.network.SessionManager;
import com.bitpub.network.RispostaHateoas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller per la gestione della vista di Login.
 * 
 * Implementato secondo il paradigma "HATEOAS-driven": il controller agisce come un client 
 * passivo che non possiede conoscenza statica degli endpoint di autenticazione, ma li 
 * scopre dinamicamente interrogando il punto d'ingresso (Root) delle API.
 * 
 * Gestisce l'intero ciclo di vita dell'autenticazione, dalla validazione formale
 * all'aggiornamento della sessione globale.
 *
 * @author Stefano Bellan
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label erroreLabel;

    /** Client HTTP per le operazioni di rete */
    private final RestClient restClient = RestClient.getInstance();

    /**
     * Gestisce il processo di autenticazione dell'utente.
     * 
     * Il workflow segue tre fasi asincrone:
     * 1. Discovery: recupero dei metadati dalla Root API.
     * 2. Action: invio delle credenziali all'endpoint scoperto.
     * 3. Sync: aggiornamento del SessionManager e switch della scena.
     */
    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // --- VALIDAZIONE LOCALE (UI Thread) ---
        if (username.isEmpty() || password.isEmpty()) {
            erroreLabel.setText("Inserisci username e password.");
            return;
        }

        // Fornisce feedback visivo immediato per migliorare la UX durante l'attesa di rete
        erroreLabel.setText("Accesso in corso...");

        // --- FASE 1: DISCOVERY (Asincrona) ---
        // Interroga la root per identificare dinamicamente l'URL del servizio di login
        restClient.getAsync(restClient.getRootUrl(), RispostaHateoas.class)
                .thenCompose(root -> {
                    // Estrazione URL ipermediale tramite discovery sicura
                    String loginUrl = root.getLinkSafe("login");

                    // --- FASE 2: AZIONE (POST) ---
                    // Inoltra le credenziali all'indirizzo ottenuto dalla discovery
                    AuthRequest request = new AuthRequest(username, password);
                    return restClient.postAsync(loginUrl, request, AuthResponse.class);
                })
                .thenAccept(authResponse -> {
                    // --- FASE 3: ELABORAZIONE RISPOSTA ---
                    if (authResponse != null && authResponse.getToken() != null) {
                        
                        // Persistenza dei dati di sessione (Token JWT e privilegi)
                        SessionManager.getInstance().setJwtToken(authResponse.getToken());
                        SessionManager.getInstance().setUserRole(authResponse.getRole());
                        SessionManager.getInstance().setUsername(username);

                        // --- UI UPDATE ---
                        // Le modifiche allo Stage di JavaFX devono avvenire sul thread applicativo dedicato
                        Platform.runLater(() -> Main.redirectDopoLogin());
                        
                    } else {
                        // Gestione anomalie del payload di risposta (es. server non conforme)
                        Platform.runLater(() -> erroreLabel.setText("Errore: risposta del server non valida."));
                    }
                })
                .exceptionally(ex -> {
                    // --- GESTIONE ERRORI (Worker Thread) ---
                    Platform.runLater(() -> {
                        // Log tecnico su console per il debug
                        System.err.println("[LOGIN REFACTOR] Errore durante il processo: " + ex.getMessage());
                        
                        // Messaggio semplificato per l'utente finale.
                        // I codici HTTP specifici (es. 401) sono intercettati a monte dal RestClient.
                        erroreLabel.setText("Impossibile accedere: credenziali errate o server offline.");
                    });
                    return null;
                });
    }

    /**
     * Reindirizza l'utente alla schermata di creazione di un nuovo account.
     */
    @FXML
    private void vaiARegistrazione() {
        Main.navigaVerso("/RegistrazioneView.fxml", "BitPub - Registrazione");
    }
}
