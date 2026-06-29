package com.bitpub.auth.contracts;

import com.google.gson.annotations.Expose;

/**
 * Payload per la richiesta di autenticazione (Login o Register).
 *
 * @author BitPub Team
 * @version 1.0
 */
public class AuthRequest {

    @Expose
    private String username;

    @Expose
    private String password;

    @Expose
    private String email;

    public AuthRequest() {}

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public AuthRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}


