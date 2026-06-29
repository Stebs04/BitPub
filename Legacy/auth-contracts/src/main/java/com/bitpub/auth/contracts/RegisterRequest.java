package com.bitpub.auth.contracts;

import com.google.gson.annotations.Expose;

/**
 * Richiesta di registrazione utente.
 */
public class RegisterRequest {

    @Expose
    private String username;

    @Expose
    private String password;

    @Expose
    private String email;

    public RegisterRequest() {}

    public RegisterRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}


