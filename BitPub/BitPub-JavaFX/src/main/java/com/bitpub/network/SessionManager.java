package com.bitpub.network;

/**
 * Singleton per gestire il token JWT, il ruolo dell'utente e il locale assegnato
 * nella sessione corrente. Il salvataggio avviene solo in memoria per garantire
 * la sicurezza (State-Less sul disco).
 *
 * @author Stefano Bellan
 */
public class SessionManager {

    private static SessionManager instance;

    private String jwtToken;
    private String currentUser;
    private String currentRole;
    private Long currentLocaleId;

    private SessionManager() {
    }

    /**
     * Restituisce l'istanza unica del SessionManager (Thread-Safe).
     * @return L'istanza singleton.
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Restituisce il token JWT per l'autenticazione delle chiamate API.
     * @return Il token stringa.
     */
    public String getJwtToken() {
        return jwtToken;
    }

    /**
     * Inizializza i dati della sessione a seguito di un login con successo.
     * * @param username Nome utente.
     * @param token Token JWT fornito dal backend.
     * @param role Ruolo assegnato (ADMIN, GESTORE, etc.).
     * @param localeId ID del locale associato (opzionale).
     */
    public void setSession(String username, String token, String role, Long localeId) {
        this.currentUser = username;
        this.jwtToken = token;
        this.currentRole = role;
        this.currentLocaleId = localeId;
    }

    /**
     * Rimuove tutti i dati della sessione corrente (Logout).
     */
    public void clearSession() {
        this.currentUser = null;
        this.jwtToken = null;
        this.currentRole = null;
        this.currentLocaleId = null;
    }

    public String getCurrentUser() { return currentUser; }
    public String getCurrentRole() { return currentRole; }
    public Long getCurrentLocaleId() { return currentLocaleId; }

    /**
     * Verifica se è presente un token di sessione valido.
     * @return true se l'utente è autenticato.
     */
    public boolean isAuthenticated() {
        return jwtToken != null && !jwtToken.isEmpty();
    }
}