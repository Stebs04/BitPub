package com.bitpub.network;

import com.bitpub.models.Utente;

/**
 * Singleton per gestire il token JWT, il ruolo dell'utente e il locale assegnato
 * nella sessione corrente. Il salvataggio avviene solo in memoria per garantire
 * la sicurezza (State-Less sul disco).
 *
 * @author Stefano Bellan
 */
public class SessionManager {

    private static SessionManager instance;

    private String token;
    private String role;
    private String currentLocaleId; // Mantenuto internamente come stringa per flessibilità di parsing

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
     * Inizializza i dati della sessione a seguito di un login con successo.
     * Metodo utilizzato dal LoginController.
     *
     * @param token    Token JWT fornito dal backend.
     * @param role     Ruolo assegnato (ADMIN, GESTORE, etc.).
     * @param username Nome utente (non memorizzato in questa versione se non necessario).
     * @param localeId ID del locale associato.
     */
    public void setSession(String token, String role, String username, String localeId) {
        this.token = token;
        this.role = role;
        this.currentLocaleId = localeId;
    }

    /**
     * Restituisce il token JWT per l'autenticazione delle chiamate API.
     */
    public String getJwtToken() { 
        return token; 
    }

    /**
     * Restituisce il ruolo dell'utente nella sessione corrente.
     */
    public String getCurrentRole() { 
        return role; 
    }

    /**
     * Restituisce l'ID del locale come Long, facilitando l'uso nei controller e nei filtri.
     * @return L'ID numerico o null se non presente/valido.
     */
    public Long getCurrentLocaleId() {
        try {
            if (currentLocaleId == null || currentLocaleId.isEmpty()) return null;
            return Long.parseLong(currentLocaleId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Rimuove tutti i dati della sessione corrente (Logout).
     */
    public void clearSession() {
        this.token = null;
        this.role = null;
        this.currentLocaleId = null;
    }

    /**
     * Verifica se è presente un token di sessione valido.
     * @return true se l'utente è autenticato.
     */
    public boolean isAuthenticated() {
        return token != null && !token.isEmpty();
    }
}
