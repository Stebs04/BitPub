package com.bitpub.network;

/**
 * Gestisce la sessione utente all'interno dell'applicazione seguendo il pattern Singleton.
 * Memorizza il token JWT, il ruolo e lo username dell'utente per gestire l'autorizzazione e la navigazione.
 *
 * @author Stefano Bellan 20054330
 */
public class SessionManager {

    /** Istanza unica della classe (Singleton) */
    private static SessionManager instance;

    /** Token JSON Web per l'autenticazione delle richieste */
    private String jwtToken;

    /** Ruolo dell'utente loggato (es. ADMIN, GESTORE, UTENTE) */
    private String userRole;

    /** Nome visualizzato dell'utente (username) per l'interfaccia grafica */
    private String username;

    /** UUID dell'utente loggato (estratto dal JWT) */
    private java.util.UUID userId;

    /** ID del locale associato */
    private Long currentLocaleId;

    /**
     * Costruttore privato per impedire l'istanziazione esterna (Pattern Singleton).
     */
    private SessionManager() {
        // Inizializzazione protetta
    }

    /**
     * Restituisce l'istanza unica del SessionManager.
     *
     * @return L'istanza Singleton di {@link SessionManager}
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // --- Gestione Token JWT ---

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    // --- Gestione Ruolo Utente ---

    /**
     * Recupera il ruolo dell'utente attualmente memorizzato.
     * @return Il ruolo come {@link String} (es. "ADMIN"), o null se non impostato.
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * Memorizza il ruolo dell'utente dopo il login.
     * @param userRole Il ruolo dell'utente ricevuto dal server.
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    // --- Gestione Nome Utente (Username) ---

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public java.util.UUID getUserId() {
        return userId;
    }

    public void setUserId(java.util.UUID userId) {
        this.userId = userId;
    }

    public Long getCurrentLocaleId() {
        return currentLocaleId;
    }

    public void setCurrentLocaleId(Long currentLocaleId) {
        this.currentLocaleId = currentLocaleId;
    }

    /**
     * Termina la sessione corrente pulendo token, ruolo e username.
     */
    public void logout() {
        this.jwtToken = null;
        this.userRole = null;
        this.username = null;
        this.userId = null;
        this.currentLocaleId = null;
    }
}