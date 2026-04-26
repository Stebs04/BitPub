package com.bitpub.network;

/**
 * Singleton per gestire il token JWT e il ruolo dell'utente autenticato nella sessione corrente.
 * Il salvataggio avviene solo in memoria (State-Less sul disco).
 *
 * @author BitPub Team
 * @version 1.0
 */
public class SessionManager {

    private static SessionManager instance;

    private String jwtToken;
    private String currentUser;
    private String currentRole;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setSession(String username, String token, String role) {
        this.currentUser = username;
        this.jwtToken = token;
        this.currentRole = role;
    }

    public void clearSession() {
        this.currentUser = null;
        this.jwtToken = null;
        this.currentRole = null;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public boolean isAuthenticated() {
        return jwtToken != null && !jwtToken.isEmpty();
    }
}
