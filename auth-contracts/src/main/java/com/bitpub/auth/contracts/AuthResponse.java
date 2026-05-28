package com.bitpub.auth.contracts;
import com.bitpub.contracts.api.ResourceModel;

import com.google.gson.annotations.Expose;

/**
 * Payload per la risposta di autenticazione contenente username, token JWT e ruolo.
 * Estende ResourceModel per supportare i link HATEOAS nel frontend.
 *
 * @author Stefano Bellan 20054330
 */
public class AuthResponse extends ResourceModel {

    // L'annotazione @Expose indica a Gson che questo campo deve essere letto/scritto nel JSON
    @Expose
    private String username;

    @Expose
    private String token;

    // Modificato da "ruolo" a "role" per coerenza con il JSON del server
    // e con il metodo getRole() del LoginController.
    @Expose
    private String role;

    /**
     * Costruttore vuoto di default.
     * È essenziale per Gson, che lo utilizza per "costruire" l'oggetto
     * vuoto prima di riempirlo con i dati del JSON.
     */
    public AuthResponse() {}

    /**
     * Costruttore con parametri.
     * Utile lato Server (Cloud) per generare facilmente la risposta.
     */
    public AuthResponse(String username, String token, String role) {
        this.username = username;
        this.token = token;
        this.role = role;
    }

    // --- Metodi Getter e Setter ---
    // Servono per leggere (get) e modificare (set) in modo sicuro le variabili private.

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

