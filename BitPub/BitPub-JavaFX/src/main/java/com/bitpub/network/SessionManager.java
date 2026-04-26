package com.bitpub.network;

/**
 * Singleton per gestire il token JWT, il ruolo dell'utente e il locale assegnato
 * nella sessione corrente. Il salvataggio avviene solo in memoria (State-Less sul disco).
 *
 * @author Stefano Bellan 20054330
 */
public class SessionManager {

    private static SessionManager instance;

    private String jwtToken;
    private String currentUser;
    private String currentRole;
    private Long currentLocaleId; // Aggiunto per gestire il locale del gestore

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

    // Aggiunto un parametro per il localeId (può essere null per l'ADMIN o UTENTE_BASE)
    public void setSession(String username, String token, String role, Long localeId) {
        this.currentUser = username;
        this.jwtToken = token;
        this.currentRole = role;
        this.currentLocaleId = localeId;
    }

    public void clearSession() {
        this.currentUser = null;
        this.jwtToken = null;
        this.currentRole = null;
        this.currentLocaleId = null;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public Long getCurrentLocaleId() {
        return currentLocaleId;
    }

    public boolean isAuthenticated() {
        return jwtToken != null && !jwtToken.isEmpty();
    }
}