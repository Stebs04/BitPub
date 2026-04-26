package com.bitpub.models;

import com.google.gson.annotations.Expose;

/**
 * Payload per la risposta di autenticazione contenente username, token JWT e ruolo.
 * Estende ResourceModel per supportare i link HATEOAS nel frontend.
 *
 * @author BitPub Team
 * @version 1.0
 */
public class AuthResponse extends ResourceModel {

    @Expose
    private String username;

    @Expose
    private String token;

    @Expose
    private String ruolo;

    public AuthResponse() {}

    public AuthResponse(String username, String token, String ruolo) {
        this.username = username;
        this.token = token;
        this.ruolo = ruolo;
    }

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

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }
}
